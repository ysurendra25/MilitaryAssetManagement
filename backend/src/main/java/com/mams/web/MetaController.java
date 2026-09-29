package com.mams.web;

import com.mams.repository.BaseRepository;
import com.mams.repository.EquipmentTypeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Reference data for dropdowns and filters. */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final BaseRepository bases;
    private final EquipmentTypeRepository equipmentTypes;

    public MetaController(BaseRepository bases, EquipmentTypeRepository equipmentTypes) {
        this.bases = bases;
        this.equipmentTypes = equipmentTypes;
    }

    @GetMapping("/bases")
    public List<Map<String, Object>> bases() {
        return bases.findAll().stream()
                .map(b -> Map.<String, Object>of("id", b.getId(), "name", b.getName()))
                .toList();
    }

    @GetMapping("/equipment-types")
    public List<Map<String, Object>> equipmentTypes() {
        return equipmentTypes.findAll().stream()
                .map(e -> Map.<String, Object>of("id", e.getId(), "name", e.getName()))
                .toList();
    }
}
