## Single-threaded deadlock (`Deadlock1.java`, `jstack` output in `d1.out`)

Строки 14-16:
```
"main" #3 [7294] prio=5 os_prio=0 cpu=715,82ms elapsed=1,26s tid=0x00007f49f8019580 nid=7294 in Object.wait()  [0x00007f49fddfe000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait0(java.base@25.0.2/Native Method)
	- waiting on <0x0000000719c01f28> (a java.lang.Thread)
```

Т.к. поток в программе ровно один, ждёт `main` самого себя, `jstack` подтверждает состояние ожидания.

## Double-threaded deadlock (`Deadlock2.java`, `jstack` output in `d2.out`)

Строки 183-186:
```
"Thread-0" #34 [8476] prio=5 os_prio=0 cpu=0,34ms elapsed=0,41s tid=0x00007f86c8493100 nid=8476 in Object.wait()  [0x00007f867ed01000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait0(java.base@25.0.2/Native Method)
	- waiting on <0x00000007180dc258> (a java.lang.Thread)
```

Строки 201-204:
```
"Thread-1" #35 [8478] prio=5 os_prio=0 cpu=0,26ms elapsed=0,41s tid=0x00007f86c8494160 nid=8478 in Object.wait()  [0x00007f867ec00000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait0(java.base@25.0.2/Native Method)
	- waiting on <0x00000007180d4db8> (a java.lang.Thread)
```

Два потока действительно `WAITING`. Ниже ссылки на строки в исходном файле показывают, что это действительно два `join`'а.
