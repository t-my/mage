package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.Effect;
import mage.abilities.effects.common.UntapTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.target.TargetPermanent;
import mage.target.targetadjustment.XTargetsCountAdjuster;

import java.util.UUID;

/**
 * @author duncant
 */
public final class MagusOfTheCandelabra extends CardImpl {

    public MagusOfTheCandelabra(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{G}");
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WIZARD);

        this.power = new MageInt(1);
        this.toughness = new MageInt(2);

        // {X}, {T}: Untap X target lands.
        Effect effect = new UntapTargetEffect();
        effect.setText("untap X target lands");
        Ability ability = new SimpleActivatedAbility(effect, new ManaCostsImpl<>("{X}"));
        ability.addCost(new TapSourceCost());
        ability.addTarget(new TargetPermanent(StaticFilters.FILTER_LANDS));
        ability.setTargetAdjuster(new XTargetsCountAdjuster());
        this.addAbility(ability);
    }

    private MagusOfTheCandelabra(final MagusOfTheCandelabra card) {
        super(card);
    }

    @Override
    public MagusOfTheCandelabra copy() {
        return new MagusOfTheCandelabra(this);
    }
}
