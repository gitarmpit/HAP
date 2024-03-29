//
// Plugins.hpp
// Homekit
//
//  Created on: 08.08.2017
//      Author: michael
//


#ifndef PLUGINS_HPP_
#define PLUGINS_HPP_

#include "HAPGlobals.hpp"

// Add here all includes for the sensor plugins

#if HAP_PLUGIN_USE_DHT
#include "HAPPluginDHT.hpp"
#endif

#if HAP_PLUGIN_USE_LED
#include "LED/HAPPluginLED.hpp"
#endif

#if HAP_PLUGIN_USE_SWITCH
#include "HAPPluginSwitch.hpp"
#endif

#if HAP_PLUGIN_USE_MIFLORA
#include "HAPPluginMiFlora.hpp"
#endif

#if HAP_PLUGIN_USE_MIFLORA2
#include "HAPPluginMiFlora2.hpp"
#endif

#if HAP_PLUGIN_USE_BME280
#include "HAPPluginBME280.hpp"
#endif

#if HAP_PLUGIN_USE_INFLUXDB
#include "HAPPluginInfluxDB.hpp"
#endif

#if HAP_PLUGIN_USE_SSD1331
#include "HAPPluginSSD1331.hpp"
#endif

#if HAP_PLUGIN_USE_SSD1306
#include "HAPPluginSSD1306.hpp"
#endif

#if HAP_PLUGIN_USE_PCA301
#include "HAPPluginPCA301.hpp"
#endif

#if HAP_PLUGIN_USE_NEOPIXEL
#include "HAPPluginNeoPixel.hpp"
#endif

#if HAP_PLUGIN_USE_RCSWITCH
#include "HAPPluginRCSwitch.hpp"
#endif

#if HAP_PLUGIN_USE_HYGROMETER
#include "HAPPluginHygrometer.hpp"
#endif

#endif /* PLUGINS_HPP_ */  