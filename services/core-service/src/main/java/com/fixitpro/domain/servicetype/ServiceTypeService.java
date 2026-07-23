package com.fixitpro.domain.servicetype;

import com.fixitpro.common.exception.DuplicateResourceException;
import com.fixitpro.common.exception.ResourceNotFoundException;
import com.fixitpro.domain.servicetype.dto.ServiceTypeRequest;
import com.fixitpro.domain.servicetype.dto.ServiceTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> listActive() {
        return serviceTypeRepository.findByActiveTrue().stream()
                .map(ServiceTypeResponse::from)
                .toList();
    }

    @Transactional
    public ServiceTypeResponse create(ServiceTypeRequest request) {
        serviceTypeRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(request.name()))
                .findAny()
                .ifPresent(s -> { throw new DuplicateResourceException("A service type with this name already exists"); });

        ServiceType serviceType = ServiceType.builder()
                .name(request.name())
                .description(request.description())
                .basePrice(request.basePrice())
                .active(true)
                .build();

        return ServiceTypeResponse.from(serviceTypeRepository.save(serviceType));
    }

    @Transactional
    public ServiceTypeResponse update(Long id, ServiceTypeRequest request) {
        ServiceType serviceType = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + id));

        serviceType.setName(request.name());
        serviceType.setDescription(request.description());
        serviceType.setBasePrice(request.basePrice());

        return ServiceTypeResponse.from(serviceType);
    }

    @Transactional
    public void deactivate(Long id) {
        ServiceType serviceType = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + id));
        serviceType.setActive(false);
    }

    @Transactional(readOnly = true)
    public ServiceType getEntityOrThrow(Long id) {
        return serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + id));
    }
}
