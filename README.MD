###Description
Allows usage of razer-keyboard as a joystick/keyboard hybrid in analog mode (driver mode) on linux.

###Features
1. mapping keyboard keys to abitrary joystick axes/buttons
2. simultaneous use of default keyboard behaviour and joystick/gamepad
3. toggleable joystick/gamepad

###usage
FN+PAGE_UP: enable joystick (disables regular keyboard output for the mapped keys)
FN+PAGE_DOWN: disable joystick (keyboard functions as usual)
FN+HOME key combo: closes application

## prerequisites
add udev rules file /etc/udev/rules.d/\<filename-here>.rule <br/>
HuntsmanV3Pro `SUBSYSTEM=="usb",ATTR{idVendor}=="1532",ATTR{idProduct}=="02a6",MODE="0060",GROUP="plugdev"`
