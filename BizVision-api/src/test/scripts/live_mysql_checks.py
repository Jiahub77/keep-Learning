"""Test the running learning server; mutate and clean up only products created here."""
import concurrent.futures
import json
import threading
import urllib.error
import urllib.request
import uuid

BASE = 'http://localhost:8080'
created = []
checks = []

def request(method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                 headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=30) as res:
            return res.status, res.read().decode()
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode()

def check(condition, label):
    if not condition:
        raise AssertionError(label)
    checks.append(label)

def new_product(stock):
    name = 'codex-test-' + uuid.uuid4().hex[:12]
    code, body = request('POST', '/products', {'name': name, 'priceInCents': 100, 'stock': stock})
    check(code == 201, 'create returns 201')
    product_id = int(body)
    created.append(product_id)
    return product_id, name

def get_stock(product_id):
    code, body = request('GET', '/products/' + str(product_id))
    check(code == 200, 'get test product')
    return json.loads(body)['stock']

try:
    for query in ['page=0&size=2', 'page=1&size=0', 'page=1&size=101',
                  'page=2147483647&size=100', 'page=abc']:
        check(request('GET', '/products?' + query)[0] == 400, 'pagination rejects ' + query)
    code, body = request('GET', '/products?page=2147483647&size=1')
    check(code == 200 and json.loads(body)['list'] == [], 'largest safe offset returns empty list')
    product_id, name = new_product(5)
    path = '/products/decrease-stock/' + str(product_id)
    for quantity in [0, -1, 2147483648, {'quantity': 1}]:
        check(request('POST', path, quantity)[0] == 400, 'invalid quantity is 400: ' + str(quantity))
    check(get_stock(product_id) == 5, 'invalid requests preserve stock')
    check(request('POST', path, 6)[0] == 409, 'insufficient stock is 409')
    check(get_stock(product_id) == 5, 'insufficient request preserves stock')
    check(request('POST', path, 2)[0] == 200, 'valid decrement is 200')
    check(get_stock(product_id) == 3, 'valid decrement changes stock')
    update = {'name': name, 'priceInCents': 200, 'stock': 5}
    check(request('PUT', '/products/' + str(product_id), update)[0] == 200, 'update succeeds')
    check(request('PUT', '/products/' + str(product_id), update)[0] == 200, 'identical update succeeds')
    barrier = threading.Barrier(10)
    def decrement(_):
        barrier.wait(timeout=10)
        return request('POST', path, 1)[0]
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as pool:
        statuses = list(pool.map(decrement, range(10)))
    check(statuses.count(200) == 5 and statuses.count(409) == 5,
          'MySQL concurrency: exactly 5 successes and 5 conflicts from 10 requests')
    check(get_stock(product_id) == 0, 'MySQL concurrency: final stock zero')
    check(request('DELETE', '/products/' + str(product_id))[0] == 200, 'delete succeeds')
    check(request('DELETE', '/products/' + str(product_id))[0] == 404, 'repeat delete is 404')
    check(request('POST', path, 1)[0] == 404, 'missing stock product is 404')
    check(request('PUT', '/products/' + str(product_id), update)[0] == 404, 'missing update is 404')
    check(request('GET', '/products/' + str(product_id))[0] == 404, 'missing get is 404')
    print(json.dumps({'passed': len(checks), 'concurrent_statuses': statuses,
                      'created_product_ids': created}, ensure_ascii=False))
finally:
    for product_id in created:
        code, _ = request('DELETE', '/products/' + str(product_id))
        if code not in (200, 404):
            raise RuntimeError('cleanup failed for test product ' + str(product_id))
