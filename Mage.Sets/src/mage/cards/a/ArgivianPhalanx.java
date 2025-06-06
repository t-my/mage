package mage.cards.a;

import mage.MageInt;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.common.AffinityEffect;
import mage.abilities.hint.common.CreaturesYouControlHint;
import mage.abilities.keyword.VigilanceAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.common.FilterControlledCreaturePermanent;
import mage.filter.common.FilterControlledPermanent;

import java.util.UUID;

/**
 * @author TheElk801
 */
public final class ArgivianPhalanx extends CardImpl {

    static final FilterControlledPermanent filter = new FilterControlledCreaturePermanent("creatures");

    public ArgivianPhalanx(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{5}{W}");

        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.KOR);
        this.subtype.add(SubType.SOLDIER);
        this.power = new MageInt(4);
        this.toughness = new MageInt(4);

        // This spell costs {1} less to cast for each creature you control.
        this.addAbility(new SimpleStaticAbility(Zone.ALL, new AffinityEffect(filter)).addHint(CreaturesYouControlHint.instance));

        // Vigilance
        this.addAbility(VigilanceAbility.getInstance());
    }

    private ArgivianPhalanx(final ArgivianPhalanx card) {
        super(card);
    }

    @Override
    public ArgivianPhalanx copy() {
        return new ArgivianPhalanx(this);
    }
}
