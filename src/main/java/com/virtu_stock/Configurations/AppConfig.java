package com.virtu_stock.Configurations;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.virtu_stock.DTO.Response.IPOResponseDTO;
import com.virtu_stock.Models.IPO;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public ModelMapper ipoModelMapper() {

        ModelMapper mapper = new ModelMapper();

        TypeMap<IPO, IPOResponseDTO> typeMap = mapper.emptyTypeMap(IPO.class, IPOResponseDTO.class);

        typeMap.addMappings(mapping -> {
            mapping.skip(IPOResponseDTO::setStrengths);
            mapping.skip(IPOResponseDTO::setRisks);
            mapping.skip(IPOResponseDTO::setSubscriptions);
            mapping.skip(IPOResponseDTO::setGmp);
            mapping.skip(IPOResponseDTO::setAbout);
            mapping.skip(IPOResponseDTO::setRegistrar);
            mapping.skip(IPOResponseDTO::setFinancials);
        });

        typeMap.implicitMappings();

        return mapper;
    }

    @Bean
    public ModelMapper ipoGMPModelMapper() {

        ModelMapper mapper = new ModelMapper();

        TypeMap<IPO, IPOResponseDTO> typeMap = mapper.emptyTypeMap(IPO.class, IPOResponseDTO.class);

        typeMap.addMappings(mapping -> {
            mapping.skip(IPOResponseDTO::setStrengths);
            mapping.skip(IPOResponseDTO::setRisks);
            mapping.skip(IPOResponseDTO::setSubscriptions);
            mapping.skip(IPOResponseDTO::setAbout);
            mapping.skip(IPOResponseDTO::setRegistrar);
            mapping.skip(IPOResponseDTO::setFinancials);
        });

        typeMap.implicitMappings();

        return mapper;
    }

}
