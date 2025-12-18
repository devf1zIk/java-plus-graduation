package ru.yandex.practicum.pub;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.service.CompilationService;
import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
public class CompilationPublicController {

    private final CompilationService compilationService;

    @Autowired
    public CompilationPublicController(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @GetMapping
    public List<CompilationDto> getAllCompilations(@RequestParam(value = "pinned", defaultValue = "false")
                                                   boolean pinned,
                                                   @PositiveOrZero @RequestParam(value = "from", defaultValue = "0")
                                                   int from,
                                                   @Positive @RequestParam(value = "size", defaultValue = "10")
                                                   int size) {
        return compilationService.getAll(pinned, from, size);
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilation(@PathVariable long compId) {
        return compilationService.getById(compId);
    }
}
