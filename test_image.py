import http.client
import json

def test_image_endpoint():
    conn = http.client.HTTPSConnection("exercisedb.p.rapidapi.com")

    headers = {
        'x-rapidapi-key': "39d4c7d724msh48bea839fcbd664p17d019jsn67f5e1cf7dee",
        'x-rapidapi-host': "exercisedb.p.rapidapi.com",
        'Content-Type': "application/json"
    }

    print("Fetching image for exercise 0001...")
    conn.request("GET", "/image?exerciseId=0001&resolution=180", headers=headers)

    res = conn.getresponse()
    data = res.read()

    print(f"Status: {res.status}")
    print(f"Content-Type: {res.getheader('Content-Type')}")
    print(f"Data length: {len(data)} bytes")
    
    # Try to see if it's JSON or base64 or raw binary
    try:
        decoded = data.decode("utf-8")
        print("Decoded as UTF-8:")
        print(decoded[:200]) # print first 200 chars
    except Exception as e:
        print(f"Could not decode as UTF-8: {e}")
        
if __name__ == "__main__":
    test_image_endpoint()
