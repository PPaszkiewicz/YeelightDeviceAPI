[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Release](https://jitpack.io/v/PPaszkiewicz/YeelightDeviceAPI.svg)](https://jitpack.io/#User/Repo)

Api_core
=======
Implemented based on [Yeelight_Inter-Operation_Spec.pdf](https://www.yeelight.com/download/Yeelight_Inter-Operation_Spec.pdf).

Raw JAVA implementation of all classes required to connect and parse data from devices.

This module is very light (only dependencies are JSON and null-safety annotations) and should be usable in non-android projects.

Import in **build.gradle** or **build.gradle.kts** (no need to import it explicitly when using **api_kotlin**):
```gradle
implementation("com.github.PPaszkiewicz.YeelightDeviceAPI:api_core:1.0.1")
```

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