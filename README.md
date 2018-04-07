# fiscal-printers

## Sterowniki drukarek fiskalnych.

Uwaga ten kod ma ponad 10 lat (aktywnie rozwijany był w latach 2001-2007), ale ponieważ świetnie działa, postanowiłem oddać go do domeny publicznej.

Licencja: Apache 2.0, z tym, że nie wolno zmieniać stopek paragonów - Copyright ma byc tam zawarty!!!

Jesli chcesz zmienic stopki paragonow, skontaktuj sie ze mna w celu otrzymania innej licencji.

Bibllioteke najlepiej pobrac z Maven Central

```
    <groupId>name.prokop.bart.fps</groupId>
    <artifactId>drivers</artifactId>
    <version>1.0.0</version>
```

## Testowanie drukarki fiskalnej

Nalezy zbudowac pakiet testowy: ```mvn clean install -Pass```

Nastepnie w zaleznosci od tego jaka mamy drukarke wywolujemy:

```
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.ElzabMera COM1
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.ElzabOmega2 COM1
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.InnovaProfit451 COM1
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.OptimusVivo COM1
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.Posnet101 COM1
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.Thermal101 COM1
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.Thermal203 COM1
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.Thermal301 COM1
$ java -cp drivers-1.0.1-SNAPSHOT-jar-with-dependencies.jar name.prokop.bart.fps.drivers.ThermalOld COM1
```

## Uzycie z serwerem wydruku

