package com.jinxqxs.trafficcamera.service.impl;

import com.jinxqxs.trafficcamera.service.TrafficPyService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class TrafficPyServiceImpl implements TrafficPyService {
    private final String PYTHON_URL = "http://localhost:8001";

    public List<Map<String, Object>> getLiveStreams() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(PYTHON_URL + "/streams", List.class);
    }

    @Override
    public void reload() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(PYTHON_URL + "/reload", null, String.class);
    }
}
