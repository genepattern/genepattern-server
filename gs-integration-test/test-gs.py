# Add this script to cron to test GenomeSpace integration

import httplib
import smtplib

try:
    conn = httplib.HTTPSConnection("identity.genomespace.org", timeout=10)
    conn.request("HEAD", "/")
    response = conn.getresponse()
    print(response.status, response.reason)
except httplib.HTTPException as e:
    SERVER = "localhost"

    FROM = "gp-dev@broadinstitute.org"
    TO = ["gp-dev@broadinstitute.org"]  # must be a list

    SUBJECT = "GenePattern-GenomeSpace Integration Failing"

    TEXT = "An error was encountered testing GenePattern-GenomeSpace integration:\n\n" + e

    # Prepare actual message

    message = """\
    From: %s
    To: %s
    Subject: %s

    %s
    """ % (FROM, ", ".join(TO), SUBJECT, TEXT)

    # Send the mail

    server = smtplib.SMTP(SERVER)
    server.sendmail(FROM, TO, message)
    server.quit()
