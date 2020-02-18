## Linux service

To start and stop our service when the Pi starts, we need a service wrapper. We've created a service wrapper that start your Java application: `num2.service`. The service configuration can be found in:

`/lib/systemd/system/num2.service`

The service is automatically started at boot. You can manually start/stop the service using:

`sudo systemctl start num2.service`

`sudo systemctl stop num2.service`

If you modify the service configuration you have to reload the configuration:

`sudo systemctl daemon-reload`

See also [this resource](http://www.diegoacuna.me/how-to-run-a-script-as-a-service-in-raspberry-pi-raspbian-jessie/) about running scripts as services.
