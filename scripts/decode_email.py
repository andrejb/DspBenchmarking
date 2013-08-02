# decode_email.py
#
# Import email messages from imap account and decode them into parseable text.


import imaplib, getpass, email, sys, base64, StringIO, gzip, subprocess, quopri
import os


# imap configuration
IMAP_HOST = "imap.gmail.com"
IMAP_FOLDER = "resultados-enviados"


# messages configuration
SUBJECT_LABEL = "[dsp-benchmarking]"
ATTACHMENT_LABEL = "<attachment>"


# output file configuration
OUTPUT_PATH = "./results/"
OUTPUT_PREFIX = "result_"
OUTPUT_SUFFIX = ".txt"


# get imap user/pass
print "User:",
IMAP_USER = raw_input()
IMAP_PASSWORD = getpass.getpass()


# get messages
mail = imaplib.IMAP4_SSL(IMAP_HOST)
mail.login(IMAP_USER, IMAP_PASSWORD)
mail.select(IMAP_FOLDER)
result, data = mail.search(None, "ALL")
ids   = data[0].split()
ids.reverse()


count = 0
len_ids = len(ids)


for vid in ids:
    raw = mail.fetch(vid, "(RFC822)")
    msg = email.message_from_string(raw[1][0][1])

    count = count + 1
    print "[%d/%d] " % (count, len_ids),
    sys.stdout.flush()

    if SUBJECT_LABEL in msg["Subject"]:
        print "[%s] from [%s]..." % (msg["Date"], msg["From"])

        maintype = msg.get_content_maintype()
        if maintype == "multipart":
            for part in msg.get_payload():
                if part.get_content_maintype() == "text":
                    txt = msg.get_payload()[0].as_string()

                    if ATTACHMENT_LABEL in txt:
                        plain  = quopri.decodestring(txt).split(ATTACHMENT_LABEL)[1]
                        stio = StringIO.StringIO(base64.b64decode(plain))

                        try:

                            gz   = gzip.GzipFile(fileobj = stio)
                            orig = gz.read()
                            gz.close()

                            name = os.path.join(
                                OUTPUT_PATH,
                                OUTPUT_PREFIX + vid + OUTPUT_SUFFIX)
                            print " ... wrote output to", name

                            fp = open(name, "w")
                            fp.write(orig)
                            fp.flush()
                            fp.close()

                        except Exception as e:
                            print type(e)
                            print e.args
                            print "Failed to decompress message."

                    else:
                        print "Message malformed."

                    break
