import subprocess
import base64
import os

def pull_db_base64():
    # Use -w 0 to disable line wrapping in base64 if possible, or just handle it
    cmd = ['adb', '-s', '3dcdd883', 'shell', 'run-as', 'com.john.TreeApp', 'base64', 'databases/mydatabase.db']
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()
    
    if stderr:
        print(f"Stderr: {stderr.decode()}")
        
    # Remove any extra characters added by adb shell (like \r)
    # Most adb implementations add \r to every \n
    b64_data = stdout.replace(b'\r\n', b'\n').replace(b'\r', b'\n')
    
    # Actually, base64 output usually has its own line breaks. 
    # Just join all and decode.
    clean_b64 = b"".join(b64_data.split())
    
    try:
        decoded = base64.b64decode(clean_b64)
        with open('phone_db.sqlite', 'wb') as f:
            f.write(decoded)
        print(f"Successfully decoded {len(decoded)} bytes to phone_db.sqlite")
    except Exception as e:
        print(f"Error decoding base64: {e}")

if __name__ == "__main__":
    pull_db_base64()
