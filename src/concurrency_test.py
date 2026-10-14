import threading
import requests

TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzkwNzQ2OTY1LCJleHAiOjE3OTA3NTA1NjV9.yD2Jxorz0tQYo79itvEuDThuS5-qBKeEJlKZZBl2cYU"
URL = "http://localhost:8080/api/orders"
PRODUCT_ID = 2
NUM_REQUESTS = 5

results = []
barrier = threading.Barrier(NUM_REQUESTS)

def place_order(i):
    barrier.wait()  # every thread waits here until all are ready, then all release together
    resp = requests.post(
        URL,
        json={
            "items": [{"productId": PRODUCT_ID, "quantity": 1}],
            "shippingAddress": "Concurrency Test Address"
        },
        headers={"Authorization": f"Bearer {TOKEN}"}
    )
    results.append((i, resp.status_code, resp.text))

threads = [threading.Thread(target=place_order, args=(i,)) for i in range(NUM_REQUESTS)]
for t in threads:
    t.start()
for t in threads:
    t.join()

print("\n--- Results ---")
for i, status, text in sorted(results):
    print(f"Request {i}: {status}")

success_count = sum(1 for _, status, _ in results if status == 201)
print(f"\nSuccessful orders: {success_count} (expected: 1, since stock was only 1)")