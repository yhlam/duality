from Crypto.Cipher import AES
import sys
import os

KEY = "346a23652a46392b4d73257c67317e352e3372482177652c"

if __name__ == "__main__":
    code = bytes.fromhex(KEY)
    cipher = AES.new(code, 1)
    filename = sys.argv[1]
    with open(filename, 'rb') as f, open(filename + '.db', 'wb') as out:
        decoded = cipher.decrypt(f.read())
        out.write(decoded)

