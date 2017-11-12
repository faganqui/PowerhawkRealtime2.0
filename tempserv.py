import socket
import struct
import time
 
# Create a TCP/IP socket
TCP_IP = '6312.triacta.com'
TCP_PORT = 502
BUFFER_SIZE = 32
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((TCP_IP, TCP_PORT))
 
try:
    # Switch Plug On then Off
    unitId = 16 # Plug Socket
    functionCode = 5 # Write coil
 
    print("\nSwitching Plug ON...")
    coilId = 1
    req = struct.pack('12B', 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, int(unitId), int(functionCode), 0x00, int(coilId), 0xff, 0x00)
    sock.send(req)
    print("TX: (%s)" %req)
    rec = sock.recv(BUFFER_SIZE)
    print("RX: (%s)" %rec)
    time.sleep(2)
 
    print("\nSwitching Plug OFF...")
    coilId = 2
    req = struct.pack('12B', 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, int(unitId), int(functionCode), 0x00, int(coilId), 0xff, 0x00)
    sock.send(req)
    print("TX: (%s)" %req)
    rec = sock.recv(BUFFER_SIZE)
    print("RX: (%s)" %rec)
    time.sleep(2)
 
finally:
    print('\nCLOSING SOCKET')
    sock.close()