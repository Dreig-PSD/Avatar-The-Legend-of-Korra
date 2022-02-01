package ru.ckateptb.abilityslots.avatar.fire.ability.elements;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;

@Getter
@Setter
public class CombustionElement  extends AbstractAbilityCategory {

    private final String name = "Combustion";
    private String displayName = "§4Combustion";
    private String prefix = "§4";
}
