package mage.cards.s;

import java.util.Optional;
import java.util.UUID;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.ContinuousRuleModifyingEffectImpl;
import mage.abilities.effects.common.AttachEffect;
import mage.abilities.effects.common.SacrificeSourceUnlessPaysEffect;
import mage.abilities.effects.common.combat.CantAttackBlockAttachedEffect;
import mage.abilities.keyword.EnchantAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AttachmentType;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.target.TargetPermanent;
import mage.target.common.TargetCreaturePermanent;

/**
 *
 * @author L_J (based on jeffwadsworth)
 */
public final class SerraBestiary extends CardImpl {

    public SerraBestiary(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{W}{W}");
        this.subtype.add(SubType.AURA);

        // Enchant creature
        TargetPermanent auraTarget = new TargetCreaturePermanent();
        this.getSpellAbility().addTarget(auraTarget);
        this.getSpellAbility().addEffect(new AttachEffect(Outcome.Removal));
        Ability ability = new EnchantAbility(auraTarget);
        this.addAbility(ability);

        // At the beginning of your upkeep, sacrifice Serra Bestiary unless you pay {W}{W}.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(new SacrificeSourceUnlessPaysEffect(new ManaCostsImpl<>("{W}{W}"))));

        // Enchanted creature can't attack or block, and its activated abilities with {T} in their costs can't be activated.
        Ability ability2 = new SimpleStaticAbility(new CantAttackBlockAttachedEffect(AttachmentType.AURA));
        ability2.addEffect(new SerraBestiaryRuleModifyingEffect());
        this.addAbility(ability2);

    }

    private SerraBestiary(final SerraBestiary card) {
        super(card);
    }

    @Override
    public SerraBestiary copy() {
        return new SerraBestiary(this);
    }
}

class SerraBestiaryRuleModifyingEffect extends ContinuousRuleModifyingEffectImpl {

    SerraBestiaryRuleModifyingEffect() {
        super(Duration.WhileOnBattlefield, Outcome.Detriment);
        staticText = ", and its activated abilities with {T} in their costs can't be activated";
    }

    private SerraBestiaryRuleModifyingEffect(final SerraBestiaryRuleModifyingEffect effect) {
        super(effect);
    }

    @Override
    public SerraBestiaryRuleModifyingEffect copy() {
        return new SerraBestiaryRuleModifyingEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ACTIVATE_ABILITY;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        Permanent enchantment = source.getSourcePermanentOrLKI(game);
        if (enchantment == null) {
            return false;
        }
        Permanent enchantedCreature = game.getPermanent(enchantment.getAttachedTo());
        if (enchantedCreature == null) {
            return false;
        }
        MageObject object = game.getObject(event.getSourceId());
        Optional<Ability> ability = game.getAbility(event.getTargetId(), event.getSourceId());
        return ability.isPresent()
                && object != null
                && object.isCreature(game)
                && object.getId().equals(enchantedCreature.getId())
                && game.getState().getPlayersInRange(source.getControllerId(), game).contains(event.getPlayerId())
                && ability.get().hasTapCost();
    }

    @Override
    public String getInfoMessage(Ability source, GameEvent event, Game game) {
        return "Enchanted creature can't use its activated abilities that use {tap} in their costs.";
    }
}
