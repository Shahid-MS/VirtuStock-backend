package com.virtu_stock.Projection;

import java.util.UUID;

import com.virtu_stock.Models.Registrar;

public interface IPORegistrarProjection {

    UUID getIpoId();

    Registrar getRegistrar();
}
