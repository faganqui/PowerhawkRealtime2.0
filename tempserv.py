from pyfcm import FCMNotification
 
push_service = FCMNotification(api_key="AAAAorfeITw:APA91bHYChN3fw1SkXgECcgVwg59ID0lV04HOC4o9UdsFJat7QgvUIYSrvfS-4o-2bwX9--dISycYyHNoMBV_RyQanWOjL0JvY4Rzt0PLuCgvdi9jgt9jjiDAn0n0RL7Yy5a50pdAC-i")
 
registration_id = "cStX6wV7uAA:APA91bEPTtGi2paJPtPlz3tBwrWnueiAYWc7I29hkilekbPH8kxwqDNOdIMijkGNSHEowAtJXSPX_cygHYWlDgEcqN0EvdPW8Nx_98aj1QsG2qEKUDCxxW6Ptm1wEtd2hw_gtrx1"
message_title = "Uber update"
message_body = "Hi john, your customized news for today is ready"
result = push_service.notify_single_device(registration_id=registration_id, message_title=message_title, message_body=message_body)
 
print (result)
 