## prerequisites
add udev rules file /etc/udev/rules.d/\<filename-here>.rule <br/>
HuntsmanV3Pro `SUBSYSTEM=="usb",ATTR{idVendor}=="1532",ATTR{idProduct}=="02a6",MODE="0060",GROUP="plugdev"`