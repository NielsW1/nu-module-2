## Initial configuration

This entire process has already been done for you by the coaches, but is documented here in case the SD-card gets corrupted and the process has to be repeated.

### On your laptop

Clone the [nu-module-2](https://github.com/nedap/nu-module-2) repository:

`git clone git@github.com:nedap/nu-module-2.git`

`cd nu-module-2`

(This guide assumes your current working directory is the root of that Git workspace.)

Download the [Raspbian Buster Lite](https://www.raspberrypi.org/downloads/raspbian/) image and flash it onto the SD-card.

Put the SD-card into the Pi, then connect the power supply, keyboard, network cable, and monitor.

### On the Pi

Log in:

* username: `pi`
* password: `raspberry`

(This guide assumes your working directory is `~`, a.k.a. `/home/pi`.)

Use `sudo raspi-config` to change the following settings:

* 2 Network Options > N1 Hostname > `nu-pi-{name}`<br>(For `{name}`, insert your name, e.g. `robin`, to personalize the Pi's hostname.)

* 4 Localization Options >
 * I2 Change Timezone > `Europe` > `Amsterdam`
 * I3 Change Keyboard Layout > `Generic 104-key PC` > `English (US)` > `<default>` > `<default>`
 * I4 Change Wi-fi Country > `Netherlands`
* 5 Interfacing Options > P2 SSH > `Yes`

Set the date/time correctly with:

`sudo date -s "$(wget -qSO- --max-redirect=0 google.com 2>&1 | grep Date: | cut -d' ' -f5-8)Z"`

Or, if no network is available, with a command like:

`sudo date -s "19 FEB 2020 15:00:00"`.

Restart the Pi:

`sudo reboot`

### On your laptop

Copy the setup script to the Pi:

`scp pi_setup/setup.sh pi@10.10.10.10:/home/pi`

(When asked, use the password `raspberry` again. The IP-address `10.10.10.10` should be replaced by the Pi's actual wired IP-address, which you can find with `ifconfig`.)

### On the Pi

Check that the network connection is working, then execute the setup script:

`sudo ./setup.sh {name}`

(For `{name}`, insert your name, e.g. `robin`, to personalize the Pi's Wi-fi SSID.)

Once again, restart the Pi:

`sudo reboot`

### On your laptop

Connect to the Wi-fi network `nu-pi-{name}`.

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

See also [this resource](http://www.diegoacuna.me/how-to-run-a-script-as-a-service-in-raspberry-pi-raspbian-jessie/) about running scripts as services.
