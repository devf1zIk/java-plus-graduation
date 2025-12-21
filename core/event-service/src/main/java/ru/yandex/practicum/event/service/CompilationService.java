package ru.yandex.practicum.event.service;


import ru.yandex.practicum.interaction.dto.CompilationDto;
import ru.yandex.practicum.interaction.dto.NewCompilationDto;
import ru.yandex.practicum.interaction.dto.UpdateCompilationRequest;
import ru.yandex.practicum.interaction.dto.params.CompilationParamsPublic;

import java.util.List;

public interface CompilationService {

    List<CompilationDto> getCompilations(CompilationParamsPublic params);

    CompilationDto getCompilationById(Long compId);

    CompilationDto create(NewCompilationDto newCompilationDto);

    void deleteById(Long compId);

    CompilationDto update(Long compId, UpdateCompilationRequest updateCompilationRequest);
}