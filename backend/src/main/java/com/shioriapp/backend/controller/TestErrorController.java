package com.shioriapp.backend.controller;

import com.shioriapp.backend.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-errores")
public class TestErrorController {

    // 1. Simulamos que buscamos algo que no existe
    @GetMapping("/404")
    public String simularNoEncontrado() {
        throw new ResourceNotFoundException("El recurso de ShioriApp que buscas no existe en la base de datos.");
    }

    // 2. Simulamos un fallo crítico del sistema (NullPointer, error de BD, etc.)
    @GetMapping("/500")
    public String simularErrorInterno() {
        throw new RuntimeException("Simulando un error catastrófico del servidor.");
    }
}
