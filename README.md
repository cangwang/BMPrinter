# BMPrinter
BMPrinter is the first open source project from SKYSTAR and BCM

Characteristic
-------------------
1.Has Adapted most of phone with fingerprint above Android N,included XIAOMI，MEIZU，VIVO，OPPO,Samsung and google.<br/>
2.Has Adapted underscreen fingerprint,include XIAOMI,VIVO,OPPO.<br/>
3.Has Adapted some phone above Android L.<br/>


Add to Your project
-------------------
```
Gradle:
implementation project(':bmprinter')
```

Ussage
-------------------
All code is write with kotlin,so it would be best you know kotlin and java.<br/>
1.get BMPrinter<br/>
var fingerprintUtil: IFingerprintUtil = createFingerprintUtil()

2.authenticate<br/>
fingerprintUtil?.authenticate { success, errCode, errMsg -> if(success){}}

3.FingerprintFactory<br/>
you can see some status for fingerprint authentication in FingerprintFactory.<br/>


About Android P
-------------------
It has change FingerPrint api to BiometricPrompt and BiometricPrompt only can be built as a dialog.<br/>
New version has support android P.<br/>
