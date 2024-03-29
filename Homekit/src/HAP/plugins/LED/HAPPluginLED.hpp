//
// HAPPluginLED.hpp
// Homekit
//
//  Created on: 22.04.2018
//      Author: michael
//

#ifndef HAPPLUGINLED_HPP_
#define HAPPLUGINLED_HPP_

#include <Arduino.h>
#include "HAPPlugins.hpp"
#include "HAPLogger.hpp"
#include "HAPAccessory.hpp"

	

class HAPPluginLED: public HAPPlugin {
public:

	HAPPluginLED();
	HAPAccessory* initAccessory() override;
	
	bool begin();

	void setValue(int iid, String oldValue, String newValue);

	String getValue(int iid);

	void changePower(bool oldValue, bool newValue);
	void changeBrightness(int oldValue, int newValue);

	void handleImpl(bool forced=false);
	void identify( bool oldValue, bool newValue);
	
	// void handleEvents(int eventCode, struct HAPEvent eventParam);
	HAPConfigValidationResult validateConfig(JsonObject object);
	JsonObject getConfigImpl();
	void setConfigImpl(JsonObject root);

private:	
	//HAPAccessory*			_accessory;
	// HAPService*				_service;	
	boolCharacteristics* 	_powerState;
	intCharacteristics*	 	_brightnessState;

	// unsigned long 		_interval;
	// unsigned long 		_previousMillis;

	// EventManager*	_eventManager;
	// MemberFunctionCallable<HAPPlugin> listenerMemberFunctionPlugin;
	uint8_t _gpio;
	bool _isOn;
};

REGISTER_PLUGIN(HAPPluginLED)

#endif /* HAPPLUGINLED_HPP_ */ 