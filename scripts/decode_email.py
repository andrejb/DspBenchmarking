
import imaplib, getpass, email, sys, base64, StringIO, gzip

print "User: ",
user = raw_input()
passw = getpass.getpass()

mail = imaplib.IMAP4_SSL("imap.gmail.com")
mail.login(user, passw)

mail.select("INBOX")

result, data = mail.search(None, "ALL")

label = "[dsp-benchmarking]"
ids   = data[0].split()
ids.reverse()

count = 0
len_ids = len(ids)

for vid in ids:
	raw = mail.fetch(vid, "(RFC822)")
	msg = email.message_from_string(raw[1][0][1])
		
	count = count + 1
	print "\rprocessing %d/%d " % (count, len_ids),
	sys.stdout.flush()
	
	if label in msg["Subject"]:
		print "\nprocessing [%s] from [%s]" % (msg["Date"], msg["From"])
		
		maintype = msg.get_content_maintype()
		if maintype == "multipart":
			for part in msg.get_payload():
				if part.get_content_maintype() == "text":
					txt = msg.get_payload()[0].as_string()

					if "<attachment>" in txt:
						msg  = txt.split("attachment")[1].replace("\n","").replace("\r","")
						stio = StringIO.StringIO(base64.b64decode(msg))

						try:

							gz   = gzip.GzipFile(fileobj = stio)
							orig = gz.read()
							gz.close()

							print "compressed: " + str(len(msg))
							print "original: " + str(len(orig))

							name = "result_" + vid + ".txt"
							print " ==> ", name
							print ""

							fp = open(name, "w")
							fp.write(orig)
							fp.close()

						except:
							print "Failed to decompress message\n"						

					else:
						print "Message misformed"

					break
