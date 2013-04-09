import sqlite3
from argparse import ArgumentParser
from datetime import datetime


def import_db(in_db, out_db, my_id):
    with sqlite3.connect(in_db) as in_con, sqlite3.connect(out_db) as out_con:
        in_cur = in_con.cursor()
        out_cur = out_con.cursor()

        try:
            out_cur.execute("select max(id) as max_id from chatlog")
            next_id = out_cur.fetchone()[0] + 1
        except:
            out_cur.execute("drop table if exists chatlog")
            out_cur.execute("CREATE TABLE chatlog (id numeric, datetime numeric, senderName text, recipentName text, message text)")
            next_id = 1;

        try:
            in_cur.execute("select * from messages")
            data = fetch_data_from_android(in_cur, my_id, next_id)
        except:
            data = fetch_data_from_iphone(in_cur, my_id, next_id)

        out_cur.executemany("insert into chatlog(id, datetime, senderName, recipentName, message) values(?, ?, ?, ?, ?)", data)


def fetch_data_from_android(in_cur, my_id, next_id):
    in_cur.execute("select key_from_me from_me, key_remote_jid as jid, timestamp, data from messages where data is not null")
    messages = in_cur.fetchall()
    
    for msg in messages:
        from_me = msg[0]
        sender = my_id if from_me else msg[1]
        receiver = msg[1] if from_me else my_id

        if '@g.us' in sender or '@g.us' in receiver:
            continue

        time = msg[2]
        text = msg[3]
        
        yield (next_id, time, sender, receiver, text)

        next_id += 1


def fetch_data_from_iphone(in_cur, my_id, next_id):
    in_cur.execute("select zfromjid, ztojid, zmessagedate, ztext from zwamessage where ztext is not null")
    data = in_cur.fetchall()
    for msg in data:
        sender = msg[0] if msg[0] else my_id
        receiver = msg[1] if msg[1] else my_id

        if '@g.us' in sender or '@g.us' in receiver:
            continue

        time = (msg[2] + 11323 * 60 * 1440) * 1000
        text = msg[3]
        
        yield (next_id, time, sender, receiver, text)
        next_id += 1

        
if __name__ == "__main__":
    parser = ArgumentParser(description='Import Whatsapp data into Sqlite DB')
    parser.add_argument('in_dbs', nargs='+', help='Whatsapp')
    parser.add_argument('out_db', help='Our DB')
    args = parser.parse_args()

    for in_db in args.in_dbs:
        dot_index = in_db.rfind('.')
        my_id = in_db[:dot_index]
        import_db(in_db, args.out_db, my_id)
