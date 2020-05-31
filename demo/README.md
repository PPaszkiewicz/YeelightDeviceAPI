[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Release](https://jitpack.io/v/PPaszkiewicz/YeelightDeviceAPI.svg)](https://jitpack.io/#PPaszkiewicz/YeelightDeviceAPI)

Demo
==========

Demo app of **api_kotlin**.

Includes:

1. **Simple demo**: showcases ```YeelightActiveViewModel``` without extra code.

2. **Direct demo**: showcases how to create connection to a single device with known address.

3. **Live demo**: showcases ```YeelightActiveViewModel```.

4. **Auto demo**: showcases ```YeelightCustomViewModel``` backed by ```YeelightAutoConnection```.

5. **No scan demo**: similar to auto but does not automatically scan devices.

Note that it's possible this demo will not work properly on emulators due to port forwarding issues (broadcast/announcement 
packets will not be received). Recommend using physical device that connects to the same wifi as Yeelight devices.

![Screenshot](screenshot.png)

## License
Copyright 2019 Paweł Paszkiewicz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
