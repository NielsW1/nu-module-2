## Initial configuration

### On your laptop

Clone the [nu-module-2](https://github.com/nedap/nu-module-2) repository:

`git clone git@github.com:nedap/nu-module-2.git`

`cd nu-module-2`

(This guide assumes your current working directory is the root of that Git workspace.)

Go to [Raspberry Pi OS](https://www.raspberrypi.com/software/) to download the Raspberry Pi Imager and use it to install Raspberry Pi OS (Lite) 64-bit on your SD card. 
You can either install Raspberry Pi OS (1.1GB, with Raspberry Pi Desktop) or Raspberry Pi OS Lite (0.4GB, no desktop environment).

Installation steps:
* Run the Raspberry Pi Imager
* Select your Raspberry Pi Device, choose your preferred Raspberry Pi OS and select the SD card as storage
* Click Next
* Click _Edit Settings_ to add OS customisations
  * Under the _General_ tab:
    * Check _Set hostname_ and enter `nu-pi-{name}` (For `{name}`, insert your name, e.g. `bob`, to personalize the Pi's hostname)
    * Check _Set username and password_ with Username `pi` and Password `raspberry`
    * **DO NOT** configure wireless LAN, this will be handled later with a custom script!
    * Check _Set locale settings_ with Time zone `Europe/Amsterdam` and Keyboard layout `us`
  * Under the _Services_ tab:
    * Check _Enable SSH_ with _Use password authentication_
* Click Save
* Click _Yes_ to use the OS customisation settings
* Click _Yes_ to continue (will erase your SD) and wait for the installation to complete

Put the SD-card into the Pi, then connect the power supply, keyboard, **network cable**, and monitor.

### On your laptop

Copy the setup script to the Pi:

`scp pi_setup/setup.sh pi@10.10.10.10:/home/pi`

When asked, use the password `raspberry` again. The IP-address `10.10.10.10` should be replaced by the Pi's actual 
wired IP-address, which you can find with the command `ifconfig` on the Pi.

If you get the error `pi@10.10.10.10: Permission denied (publickey,password).` without the option to enter your 
password, you should add the following two lines to the top of the `.ssh/config` file on your laptop. Replace the 
`10.10.10.10` IP-address, but keep `172.16.1.1` in there. 

```
Host 10.10.10.10 172.16.1.1
  PasswordAuthentication yes
```

### On the Pi

If you copied the setup.sh file using a Windows system, you may need to execute the following two steps:

* Change the line endings from Windows style (CRLF) to Unix style (LF). This can be done by a text editor or with the 
  command `sed -i 's/\r$//' setup.sh`.
* Mark the file as executable. This can be done with the command `chmod +x setup.sh`.

Check that the network connection is working, then execute the setup script:

`sudo ./setup.sh {name}`

For `{name}`, insert your name, e.g. `bob`, to personalize the Pi's Wi-Fi SSID.

This script will install all the required software and will configure the Pi to act as a Wi-Fi accesspoint.

After the installation, reboot your Pi: `sudo reboot`

### On your laptop

Connect to the Wi-Fi network `nu-pi-{name}`. The default password is `nedap1234`. If you don't see the accesspoint you
may need to disable and re-enable the Wi-Fi on your laptop.

Build and deploy the sample project `NUM2.jar`:

`./gradlew deploy`

### On the Pi

Check that the `NUM2.jar` was copied:

`ls -l`

Check that the example project is running as service `num2`:

`systemctl status num2`



## Linux service

To start and stop our service when the Pi starts, we need a service wrapper. We've created a service wrapper that start your Java application: `num2.service`. The service configuration can be found in:

`/lib/systemd/system/num2.service`

The service is automatically started at boot. You can manually start/stop the service using:

`sudo systemctl start num2.service`

`sudo systemctl stop num2.service`

If you modify the service configuration you have to reload the configuration:

`sudo systemctl daemon-reload`
