package ru.ckateptb.abilityslots.avatar.earth.ability.subcategories;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.user.AbilityUser;

@Getter
@Setter
@AbilityInfo(
        author = "CKATEPTb",
        name = "SandBender",
        displayName = "SandBender",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "earth",
        description = "This passive ability allows EarthBender to manipulate the sand.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class SandBender implements Ability {
    private AbilityUser user;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }
}