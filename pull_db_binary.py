import subprocess
import os

def pull_db():
    cmd = ['adb', '-s', '3dcdd883', 'shell', 'run-as', 'com.john.TreeApp', 'cat', 'databases/mydatabase.db']
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()
    
    if stderr and not stderr.startswith(b'Error'): # some adb warnings come on stderr
        print(f"Stderr: {stderr.decode()}")
        
    with open('phone_db.sqlite', 'wb') as f:
        f.write(stdout)
    
    print(f"Pulled {len(stdout)} bytes to phone_db.sqlite")

if __name__ == "__main__":
    pull_db()
