package com.ba.escuchador.ejb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ServiceGPISwiftV5Config {
    private static final Logger logger = LoggerFactory.getLogger(ServiceGPISwiftV5Config.class.getName());

    @Autowired
    private ServiceGPISwiftV5Listener asyncSv;

    @JmsListener(destination = "${ibm.queue.request}")
    public void receiveIbmMq(Object message){
        logger.info("Recibiendo mensaje en la cola");
        asyncSv.onMessage(message);
    }
}
