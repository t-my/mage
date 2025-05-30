package mage.cards;

import mage.ObjectColor;
import mage.cards.repository.CardCriteria;
import mage.cards.repository.CardInfo;
import mage.cards.repository.CardRepository;
import mage.collation.BoosterCollator;
import mage.constants.CardType;
import mage.constants.Rarity;
import mage.constants.SetType;
import mage.constants.SuperType;
import mage.filter.FilterMana;
import mage.util.CardUtil;
import mage.util.RandomUtil;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author BetaSteward_at_googlemail.com
 */
public abstract class ExpansionSet implements Serializable {

    private static final Logger logger = Logger.getLogger(ExpansionSet.class);

    // TODO: remove all usage to default (see below), keep bfz/zen/ust art styles for specific sets only
    //  the main different in art styles - full art lands can have big mana icon at the bottom
    public static final CardGraphicInfo FULL_ART_BFZ_VARIOUS = new CardGraphicInfo(FrameStyle.BFZ_FULL_ART_BASIC, true);
    public static final CardGraphicInfo FULL_ART_ZEN_VARIOUS = new CardGraphicInfo(FrameStyle.ZEN_FULL_ART_BASIC, true);
    public static final CardGraphicInfo FULL_ART_UST_VARIOUS = new CardGraphicInfo(FrameStyle.UST_FULL_ART_BASIC, true);

    // default art styles in single set:
    // - normal M15/image art (default)
    // - normal M15/image art for multiple cards in set with same name
    // - full art
    // - full art for multiple cards in set with same name
    // TODO: find or implement really full art in m15 render mode (without card name header)
    public static final CardGraphicInfo NORMAL_ART = null;
    public static final CardGraphicInfo NON_FULL_USE_VARIOUS = new CardGraphicInfo(null, true); // TODO: rename to NORMAL_ART_USE_VARIOUS
    public static final CardGraphicInfo RETRO_ART = new CardGraphicInfo(FrameStyle.RETRO, false);
    public static final CardGraphicInfo RETRO_ART_USE_VARIOUS = new CardGraphicInfo(FrameStyle.RETRO, true);
    public static final CardGraphicInfo FULL_ART = new CardGraphicInfo(FrameStyle.MPOP_FULL_ART_BASIC, false);
    public static final CardGraphicInfo FULL_ART_USE_VARIOUS = new CardGraphicInfo(FrameStyle.MPOP_FULL_ART_BASIC, true);

    // TODO: enable after mutate implementation
    public static final boolean HIDE_MUTATE_CARDS = true;
    public static final Set<String> MUTATE_CARD_NAMES = new HashSet<>(Arrays.asList(
            "Archipelagore",
            "Auspicious Starrix",
            "Boneyard Lurker",
            "Cavern Whisperer",
            "Chittering Harvester",
            "Cloudpiercer",
            "Cubwarden",
            "Dirge Bat",
            "Dreamtail Heron",
            "Everquill Phoenix",
            "Gemrazer",
            "Glowstone Recluse",
            "Huntmaster Liger",
            "Illuna, Apex of Wishes",
            "Insatiable Hemophage",
            "Lore Drakkis",
            "Majestic Auricorn",
            "Mindleecher",
            "Migratory Greathorn",
            "Necropanther",
            "Nethroi, Apex of Death",
            "Otrimi, the Ever-Playful",
            "Parcelbeast",
            "Porcuparrot",
            "Pouncing Shoreshark",
            "Regal Leosaur",
            "Sawtusk Demolisher",
            "Sea-Dasher Octopus",
            "Snapdax, Apex of the Hunt",
            "Souvenir Snatcher",
            "Sawtusk Demolisher",
            "Trumpeting Gnarr",
            "Vadrok, Apex of Thunder",
            "Vulpikeet"
    ));

    public static class SetCardInfo implements Serializable {

        private final String name;
        private final String cardNumber;
        private final Rarity rarity;
        private final Class<?> cardClass;
        private final CardGraphicInfo graphicInfo;

        public SetCardInfo(String name, int cardNumber, Rarity rarity, Class<?> cardClass) {
            this(name, String.valueOf(cardNumber), rarity, cardClass, null);
        }

        public SetCardInfo(String name, String cardNumber, Rarity rarity, Class<?> cardClass) {
            this(name, cardNumber, rarity, cardClass, null);
        }

        public SetCardInfo(String name, int cardNumber, Rarity rarity, Class<?> cardClass, CardGraphicInfo graphicInfo) {
            this(name, String.valueOf(cardNumber), rarity, cardClass, graphicInfo);
        }

        public SetCardInfo(String name, String cardNumber, Rarity rarity, Class<?> cardClass, CardGraphicInfo graphicInfo) {
            this.name = name;
            this.cardNumber = cardNumber;
            this.rarity = rarity;
            this.cardClass = cardClass;
            this.graphicInfo = graphicInfo;
        }

        public String getName() {
            return this.name;
        }

        public String getCardNumber() {
            return this.cardNumber;
        }

        public int getCardNumberAsInt() {
            return CardUtil.parseCardNumberAsInt(this.cardNumber);
        }

        public Rarity getRarity() {
            return this.rarity;
        }

        public Class<?> getCardClass() {
            return this.cardClass;
        }

        public CardGraphicInfo getGraphicInfo() {
            return this.graphicInfo;
        }

        public boolean isFullArt() {
            return this.graphicInfo != null
                    && this.graphicInfo.getFrameStyle() != null
                    && this.graphicInfo.getFrameStyle().isFullArt();
        }

        public boolean isRetroFrame() {
            return this.graphicInfo != null
                    && this.graphicInfo.getFrameStyle() != null
                    && (this.graphicInfo.getFrameStyle() == FrameStyle.RETRO
                    || this.graphicInfo.getFrameStyle() == FrameStyle.LEA_ORIGINAL_DUAL_LAND_ART_BASIC);
        }
    }

    private enum ExpansionSetComparator implements Comparator<ExpansionSet> {
        instance;

        @Override
        public int compare(ExpansionSet lhs, ExpansionSet rhs) {
            return lhs.getReleaseDate().after(rhs.getReleaseDate()) ? -1 : 1;
        }
    }

    public static ExpansionSetComparator getComparator() {
        return ExpansionSetComparator.instance;
    }

    protected final List<SetCardInfo> cards = new ArrayList<>();

    protected String name;
    protected String code;
    protected Date releaseDate;
    protected ExpansionSet parentSet; // used to search additional lands and reprints for booster
    protected SetType setType;

    // TODO: 03.10.2018, hasBasicLands can be removed someday -- it's uses to optimize lands search in deck generation and lands adding (search all available lands from sets)
    protected boolean hasBasicLands = true;

    protected String blockName; // used to group sets in some GUI dialogs like choose set dialog
    protected boolean rotationSet = false; // used to determine if a set is a standard rotation
    protected boolean hasBoosters = false;
    protected int numBoosterSpecial;

    protected int numBoosterLands;

    // if ratioBoosterSpecialLand > 0, one basic land may be replaced with a special card
    // with probability ratioBoosterSpecialLandNumerator / ratioBoosterSpecialLand
    protected int ratioBoosterSpecialLand = 0;
    protected int ratioBoosterSpecialLandNumerator = 1;

    // if ratioBoosterSpecialCommon > 0, one common may be replaced with a special card
    // with probability 1 / ratioBoosterSpecialCommon
    protected int ratioBoosterSpecialCommon = 0;

    // if ratioBoosterSpecialRare > 0, one uncommon or rare is always replaced with a special card
    // probability that a rare rather than an uncommon is replaced is 1 / ratioBoosterSpecialRare
    // probability that the replacement card for a rare is a mythic is 1 / ratioBoosterSpecialMythic
    protected double ratioBoosterSpecialRare = 0;
    protected double ratioBoosterSpecialMythic;

    protected int numBoosterCommon;
    protected int numBoosterUncommon;
    protected int numBoosterRare;
    protected int numBoosterDoubleFaced; // -1 = include by rarity slots, 0 = fail on tests, 1-n = include explicit
    protected double ratioBoosterMythic;

    protected boolean hasUnbalancedColors = false;
    protected boolean hasOnlyMulticolorCards = false;
    protected boolean hasAlternateBoosterPrintings = true; // not counting basic lands; e.g. Fallen Empires true, but Tenth Edition false

    protected int maxCardNumberInBooster; // used to omit cards with collector numbers beyond the regular cards in a set for boosters

    protected final EnumMap<Rarity, List<CardInfo>> savedCards = new EnumMap<>(Rarity.class);
    protected final EnumMap<Rarity, List<CardInfo>> savedSpecialCards = new EnumMap<>(Rarity.class);
    protected Map<String, List<CardInfo>> savedReprints = null;
    protected final Map<String, CardInfo> inBoosterMap = new HashMap<>();

    protected ExpansionSet(String name, String code, Date releaseDate, SetType setType) {
        this.name = name;
        this.code = code;
        this.releaseDate = releaseDate;
        this.setType = setType;
        this.maxCardNumberInBooster = Integer.MAX_VALUE;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public int getReleaseYear() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.getReleaseDate());
        return cal.get(Calendar.YEAR);
    }

    public ExpansionSet getParentSet() {
        return parentSet;
    }

    public SetType getSetType() {
        return setType;
    }

    public String getBlockName() {
        return blockName;
    }

    public List<SetCardInfo> getSetCardInfo() {
        return cards;
    }

    @Override
    public String toString() {
        return name;
    }

    public List<SetCardInfo> findCardInfoByClass(Class<?> clazz) {
        return cards.stream().filter(info -> info.getCardClass().equals(clazz)).collect(Collectors.toList());
    }

    public List<Card> create15CardBooster() {
        // Forces 15 card booster packs.
        // if the packs are too small, it adds commons to fill it out.
        // if the packs are too big, it removes the first cards.
        // since it adds lands then commons before uncommons
        // and rares this should be the least disruptive.
        List<Card> theBooster = this.createBooster();

        if (15 > theBooster.size()) {
            List<CardInfo> commons = getCardsByRarity(Rarity.COMMON);
            while (15 > theBooster.size() && !commons.isEmpty()) {
                addToBooster(theBooster, commons);
                if (commons.isEmpty()) {
                    commons = getCardsByRarity(Rarity.COMMON);
                }
            }
        }

        while (theBooster.size() > 15) {
            theBooster.remove(0);
        }

        return theBooster;
    }

    protected void addToBooster(List<Card> booster, List<CardInfo> cards) {
        if (cards.isEmpty()) {
            return;
        }

        CardInfo cardInfo = cards.remove(RandomUtil.nextInt(cards.size()));
        Card card = cardInfo.createCard();
        if (card == null) {
            // card with error
            return;
        }

        booster.add(card);
    }

    public BoosterCollator createCollator() {
        return null;
    }

    public List<Card> createBooster() {
        BoosterCollator collator = createCollator();
        if (collator != null) {
            return createBoosterUsingCollator(collator);
        }

        for (int i = 0; i < 100; i++) {//don't want to somehow loop forever
            List<Card> booster = tryBooster();
            if (boosterIsValid(booster)) {
                return addReprints(booster);
            }
        }

        // return random booster if can't do valid
        logger.error(String.format("Can't generate valid booster for set [%s - %s]", this.getCode(), this.getName()));
        return addReprints(tryBooster());
    }

    private List<Card> createBoosterUsingCollator(BoosterCollator collator) {
        synchronized (inBoosterMap) {
            if (inBoosterMap.isEmpty()) {
                generateBoosterMap();
            }
        }
        return collator
                .makeBooster()
                .stream()
                .map(inBoosterMap::get)
                .map(CardInfo::createCard)
                .collect(Collectors.toList());
    }

    protected void generateBoosterMap() {
        CardRepository
                .instance
                .findCards(new CardCriteria().setCodes(code))
                .stream()
                .forEach(cardInfo -> inBoosterMap.put(cardInfo.getCardNumber(), cardInfo));
        // get basic lands from parent set if this set doesn't have them
        if (!hasBasicLands && parentSet != null) {
            String parentCode = parentSet.code;
            CardRepository
                    .instance
                    .findCards(new CardCriteria().setCodes(parentCode).rarities(Rarity.LAND))
                    .stream()
                    .forEach(cardInfo -> inBoosterMap.put(parentCode + "_" + cardInfo.getCardNumber(), cardInfo));
        }
    }

    protected boolean boosterIsValid(List<Card> booster) {
        if (!validateCommonColors(booster)) {
            return false;
        }

        if (!validateUncommonColors(booster)) {
            return false;
        }

        // TODO: add partner check
        // TODO: add booster size check?
        return true;
    }

    public static ObjectColor getColorForValidate(Card card) {
        ObjectColor color = card.getColor();
        // treat colorless nonland cards with exactly one ID color as cards of that color
        // (e.g. devoid, emerge, spellbombs... but not mana fixing artifacts)
        if (color.isColorless() && !card.isLand()) {
            FilterMana colorIdentity = card.getColorIdentity();
            if (colorIdentity.getColorCount() == 1) {
                return new ObjectColor(colorIdentity.toString());
            }
        }
        return color;
    }

    protected boolean validateCommonColors(List<Card> booster) {
        List<ObjectColor> commonColors = booster.stream()
                .filter(card -> card.getRarity() == Rarity.COMMON)
                .map(ExpansionSet::getColorForValidate)
                .collect(Collectors.toList());

        // for multicolor sets, count not just the colors present at common,
        // but also the number of color combinations (guilds/shards/wedges)
        // e.g. a booster with three UB commons, three RW commons and four G commons
        // has all five colors but isn't "balanced"
        ObjectColor colorsRepresented = new ObjectColor();
        Set<ObjectColor> colorCombinations = new HashSet<>();
        int colorlessCountPlusOne = 1;

        for (ObjectColor color : commonColors) {
            colorCombinations.add(color);
            int colorCount = color.getColorCount();
            if (colorCount == 0) {
                ++colorlessCountPlusOne;
            } else if (colorCount > 1 && !hasOnlyMulticolorCards) {
                // to prevent biasing toward multicolor over monocolor cards,
                // count them as one of their colors chosen at random
                List<ObjectColor> multiColor = color.getColors();
                colorsRepresented.addColor(multiColor.get(RandomUtil.nextInt(multiColor.size())));
            } else {
                colorsRepresented.addColor(color);
            }
        }

        int colors = Math.min(colorsRepresented.getColorCount(), colorCombinations.size());

        // if booster has all five colors in five unique combinations, or if it has
        // one card per color and all but one of the rest are colorless, accept it
        // ("all but one" adds some leeway for sets with small boosters)
        if (colors >= Math.min(5, commonColors.size() - colorlessCountPlusOne)) return true;
        // otherwise, if booster is missing more than one color, reject it
        if (colors < 4) return false;
        // for Torment and Judgment, always accept boosters with four out of five colors
        if (hasUnbalancedColors) return true;
        // if a common was replaced by a special card, increase the chance to accept four colors
        if (commonColors.size() < numBoosterCommon) ++colorlessCountPlusOne;

        // otherwise, stochiastically treat each colorless card as 1/5 of a card of the missing color
        return (RandomUtil.nextDouble() > Math.pow(0.8, colorlessCountPlusOne));
    }

    protected boolean validateUncommonColors(List<Card> booster) {
        List<ObjectColor> uncommonColors = booster.stream()
                .filter(card -> card.getRarity() == Rarity.UNCOMMON)
                .map(ExpansionSet::getColorForValidate)
                .collect(Collectors.toList());

        // if there are only two uncommons, they can be the same color
        if (uncommonColors.size() < 3) return true;
        // boosters of artifact sets can have all colorless uncommons
        if (uncommonColors.contains(ObjectColor.COLORLESS)) return true;
        // otherwise, reject if all uncommons are the same color combination
        return (new HashSet<>(uncommonColors).size() > 1);
    }

    protected boolean checkMythic() {
        return ratioBoosterMythic > 0 && ratioBoosterMythic * RandomUtil.nextDouble() <= 1;
    }

    protected boolean checkSpecialMythic() {
        return ratioBoosterSpecialMythic > 0 && ratioBoosterSpecialMythic * RandomUtil.nextDouble() <= 1;
    }

    /**
     * Generates a single booster by rarity ratio in sets without custom collator
     */
    protected List<Card> tryBooster() {
        // Booster generating proccess must use:
        //  * unique cards list for ratio calculation (see removeReprints)
        //  * reprints for final result (see addReprints)
        //
        // BUT there is possible a card's duplication, see https://www.mtgsalvation.com/forums/magic-fundamentals/magic-general/554944-do-booster-packs-ever-contain-two-of-the-same-card

        List<Card> booster = new ArrayList<>();
        if (!hasBoosters) {
            return booster;
        }

        if (numBoosterLands > 0) {
            List<CardInfo> specialLands = getSpecialCardsByRarity(Rarity.LAND);
            List<CardInfo> basicLands = getCardsByRarity(Rarity.LAND);
            for (int i = 0; i < numBoosterLands; i++) {
                if (ratioBoosterSpecialLand > 0 && RandomUtil.nextInt(ratioBoosterSpecialLand) < ratioBoosterSpecialLandNumerator) {
                    addToBooster(booster, specialLands);
                } else {
                    addToBooster(booster, basicLands);
                }
            }
        }

        int numCommonsToGenerate = numBoosterCommon;
        int numSpecialToGenerate = numBoosterSpecial;
        if (ratioBoosterSpecialCommon > 0 && RandomUtil.nextInt(ratioBoosterSpecialCommon) < 1) {
            --numCommonsToGenerate;
            ++numSpecialToGenerate;
        }

        List<CardInfo> commons = getCardsByRarity(Rarity.COMMON);
        for (int i = 0; i < numCommonsToGenerate; i++) {
            addToBooster(booster, commons);
        }

        int numUncommonsToGenerate = numBoosterUncommon;
        int numRaresToGenerate = numBoosterRare;
        if (ratioBoosterSpecialRare > 0) {
            Rarity specialRarity = Rarity.UNCOMMON;
            if (ratioBoosterSpecialRare * RandomUtil.nextDouble() <= 1) {
                specialRarity = (checkSpecialMythic() ? Rarity.MYTHIC : Rarity.RARE);
                --numRaresToGenerate;
            } else {
                --numUncommonsToGenerate;
            }
            addToBooster(booster, getSpecialCardsByRarity(specialRarity));
        }

        List<CardInfo> uncommons = getCardsByRarity(Rarity.UNCOMMON);
        for (int i = 0; i < numUncommonsToGenerate; i++) {
            addToBooster(booster, uncommons);
        }

        if (numRaresToGenerate > 0) {
            List<CardInfo> rares = getCardsByRarity(Rarity.RARE);
            List<CardInfo> mythics = getCardsByRarity(Rarity.MYTHIC);
            for (int i = 0; i < numRaresToGenerate; i++) {
                addToBooster(booster, checkMythic() ? mythics : rares);
            }
        }

        if (numBoosterDoubleFaced > 0) {
            addDoubleFace(booster);
        }

        if (numSpecialToGenerate > 0) {
            addSpecialCards(booster, numSpecialToGenerate);
        }

        return booster;
    }

    /* add double faced card for Innistrad booster
     * rarity near as the normal distribution
     */
    protected void addDoubleFace(List<Card> booster) {
        Rarity rarity;
        for (int i = 0; i < numBoosterDoubleFaced; i++) {
            int rarityKey = RandomUtil.nextInt(121);
            if (rarityKey < 66) {
                rarity = Rarity.COMMON;
            } else if (rarityKey < 108) {
                rarity = Rarity.UNCOMMON;
            } else if (rarityKey < 120) {
                rarity = Rarity.RARE;
            } else {
                rarity = Rarity.MYTHIC;
            }
            addToBooster(booster, getSpecialCardsByRarity(rarity));
        }
    }

    protected void addSpecialCards(List<Card> booster, int number) {
        List<CardInfo> specialCards = getCardsByRarity(Rarity.SPECIAL);
        for (int i = 0; i < number; i++) {
            addToBooster(booster, specialCards);
        }
    }

    public static Date buildDate(int year, int month, int day) {
        // The month starts with 0 = jan ... dec = 11
        return new GregorianCalendar(year, month - 1, day).getTime();
    }

    public boolean hasBoosters() {
        return hasBoosters;
    }

    public boolean hasBasicLands() {
        return hasBasicLands;
    }

    public boolean isRotationSet() {
        return rotationSet;
    }

    /**
     * Keep only unique cards for booster generation and card ratio calculation
     *
     * @param list all cards list
     */
    private List<CardInfo> removeReprints(List<CardInfo> list) {
        Map<String, CardInfo> usedNames = new HashMap<>();
        List<CardInfo> filteredList = new ArrayList<>();
        list.forEach(card -> {
            CardInfo foundCard = usedNames.getOrDefault(card.getName(), null);
            if (foundCard == null) {
                usedNames.put(card.getName(), card);
                filteredList.add(card);
            }
        });
        return filteredList;
    }

    /**
     * Fill booster with reprints, used for non collator generation
     *
     * @param booster booster's cards
     * @return
     */
    private List<Card> addReprints(List<Card> booster) {
        if (booster.stream().noneMatch(Card::getUsesVariousArt)) {
            return new ArrayList<>(booster);
        }

        // generate possible reprints
        if (this.savedReprints == null) {
            this.savedReprints = new HashMap<>();
            List<String> needSets = new ArrayList<>();
            needSets.add(this.code);
            if (this.parentSet != null) {
                // TODO: is it ok to put all parent's cards to booster instead lands only?
                needSets.add(this.parentSet.code);
            }
            List<CardInfo> cardInfos;
            if (hasAlternateBoosterPrintings) {
                cardInfos = CardRepository.instance.findCards(new CardCriteria()
                        .setCodes(needSets)
                        .variousArt(true)
                        .maxCardNumber(maxCardNumberInBooster) // ignore bonus/extra reprints
                );
            } else {
                cardInfos = CardRepository.instance.findCards(new CardCriteria()
                        .setCodes(needSets)
                        .variousArt(true)
                        .maxCardNumber(maxCardNumberInBooster) // ignore bonus/extra reprints
                        .supertypes(SuperType.BASIC) // only basic lands with extra printings
                );
            }
            cardInfos.forEach(card -> {
                this.savedReprints.putIfAbsent(card.getName(), new ArrayList<>());
                this.savedReprints.get(card.getName()).add(card);
            });
        }

        // replace normal cards by random reprints
        List<Card> finalBooster = new ArrayList<>();
        booster.forEach(card -> {
            List<CardInfo> reprints = this.savedReprints.getOrDefault(card.getName(), null);
            if (reprints != null && reprints.size() > 1) {
                Card newCard = reprints.get(RandomUtil.nextInt(reprints.size())).createCard();
                if (newCard != null) {
                    finalBooster.add(newCard);
                    return;
                }
            }
            finalBooster.add(card);
        });

        return finalBooster;
    }

    public final synchronized List<CardInfo> getCardsByRarity(Rarity rarity) {
        List<CardInfo> savedCardInfos = savedCards.get(rarity);
        if (savedCardInfos == null) {
            savedCardInfos = removeReprints(findCardsByRarity(rarity));
            savedCards.put(rarity, savedCardInfos);
        }
        // Return a copy of the saved cards information, as not to let modify the original.
        return new ArrayList<>(savedCardInfos);
    }

    public final synchronized List<CardInfo> getSpecialCardsByRarity(Rarity rarity) {
        List<CardInfo> savedCardInfos = savedSpecialCards.get(rarity);
        if (savedCardInfos == null) {
            savedCardInfos = removeReprints(findSpecialCardsByRarity(rarity));
            savedSpecialCards.put(rarity, savedCardInfos);
        }
        // Return a copy of the saved cards information, as not to let modify the original.
        return new ArrayList<>(savedCardInfos);
    }

    protected List<CardInfo> findCardsByRarity(Rarity rarity) {
        // get basic lands from parent set if this set doesn't have them
        if (rarity == Rarity.LAND && !hasBasicLands && parentSet != null) {
            return parentSet.getCardsByRarity(rarity);
        }

        List<CardInfo> cardInfos = CardRepository.instance.findCards(new CardCriteria()
                .setCodes(this.code)
                .rarities(rarity)
                .maxCardNumber(maxCardNumberInBooster));

        cardInfos.removeIf(next -> (
                next.getCardNumber().contains("*")
                        || next.getCardNumber().contains("+")));

        // special slot cards must not also appear in regular slots of their rarity
        // special land slot cards must not appear in regular common slots either
        List<CardInfo> specialCards = getSpecialCardsByRarity(rarity);
        if (rarity == Rarity.COMMON && ratioBoosterSpecialLand > 0) {
            specialCards.addAll(getSpecialCardsByRarity(Rarity.LAND));
        }
        cardInfos.removeAll(specialCards);
        return cardInfos;
    }

    /**
     * "Special cards" are cards that have common/uncommon/rare/mythic rarities
     * but can only appear in a specific slot in boosters. Examples are DFCs in
     * Innistrad sets and common nonbasic lands in many post-2018 sets.
     * <p>
     * Note that Rarity.SPECIAL and Rarity.BONUS cards are not normally treated
     * as "special cards" because by default boosters don't even have slots for
     * those rarities.
     * <p>
     * Also note that getCardsByRarity calls getSpecialCardsByRarity to exclude
     * special cards from non-special booster slots, so sets that override this
     * method must not call getCardsByRarity in it or infinite recursion will occur.
     */
    protected List<CardInfo> findSpecialCardsByRarity(Rarity rarity) {
        List<CardInfo> cardInfos = new ArrayList<>();

        // if set has special land slot, assume all common lands are special cards
        if (rarity == Rarity.LAND && ratioBoosterSpecialLand > 0) {
            cardInfos.addAll(CardRepository.instance.findCards(new CardCriteria()
                    .setCodes(this.code)
                    .rarities(Rarity.COMMON)
                    .types(CardType.LAND)
                    .maxCardNumber(maxCardNumberInBooster)));
        }

        // if set has special slot(s) for DFCs, they are special cards
        if (numBoosterDoubleFaced > 0) {
            cardInfos.addAll(CardRepository.instance.findCards(new CardCriteria()
                    .setCodes(this.code)
                    .rarities(rarity)
                    .doubleFaced(true)
                    .maxCardNumber(maxCardNumberInBooster)));
        }

        cardInfos.removeIf(next -> (
                next.getCardNumber().contains("*")
                        || next.getCardNumber().contains("+")));

        return cardInfos;
    }

    public int getMaxCardNumberInBooster() {
        return maxCardNumberInBooster;
    }

    public int getNumBoosterDoubleFaced() {
        return numBoosterDoubleFaced;
    }

    protected static void addCardInfoToList(List<CardInfo> boosterList, String name, String expansion, String cardNumber) {
        CardInfo cardInfo = CardRepository.instance.findCardWithPreferredSetAndNumber(name, expansion, cardNumber);
        if (cardInfo != null && cardInfo.getSetCode().equals(expansion) && cardInfo.getCardNumber().equals(cardNumber)) {
            boosterList.add(cardInfo);
        } else {
            throw new IllegalStateException("CardInfo not found: " + name + " (" + expansion + ":" + cardNumber + ")");
        }
    }

    /**
     * Old default booster configuration (before 2024 - MKM)
     */
    public void enableDraftBooster(int maxCardNumberInBooster, int land, int common, int uncommon, int rare) {
        // https://draftsim.com/draft-booster-vs-set-booster-mtg/
        this.hasBoosters = true;
        this.maxCardNumberInBooster = maxCardNumberInBooster;

        this.numBoosterLands = land;
        this.hasBasicLands = land > 0;

        this.numBoosterCommon = common;
        this.numBoosterUncommon = uncommon;
        this.numBoosterRare = rare;
        this.ratioBoosterMythic = 8; // 12.5% chance of a mythic rare
    }

    /**
     * Old default booster configuration (after 2020 - ZNR and before 2024 - MKM)
     */
    public void enableSetBooster(int maxCardNumberInBooster) {
        // https://draftsim.com/draft-booster-vs-set-booster-mtg/
        this.hasBoosters = true;
        this.maxCardNumberInBooster = maxCardNumberInBooster;

        this.hasBasicLands = true;
        this.numBoosterLands = 0;
        this.numBoosterCommon = 0;
        this.numBoosterUncommon = 0;
        this.numBoosterRare = 0;

        // Set boosters contain 12 cards — fewer cards than a Draft booster — but the distribution is much more complex:
        // 1 art card (5% chance of having a gold signature)
        this.numBoosterCommon += 1;
        // 1 basic land (15% chance of being foil)
        this.numBoosterLands += 1;
        // 6 commons/uncommons (different combinations possible, the most common is 4 commons and 2 uncommons)
        this.numBoosterCommon += 4;
        this.numBoosterUncommon += 2;
        // 1 unique common/uncommon
        this.numBoosterCommon += 1;
        // 2 “wild cards” (any rarity from common to mythic)
        this.numBoosterUncommon += 1;
        this.numBoosterRare += 1;
        // 1 rare (13.5% chance of being a mythic)
        this.numBoosterRare += 1;
        this.ratioBoosterMythic = 8;
        // 1 foil card
        // - ignore
        // 1 marketing card/token (25% chance of being a card from The List)
        // - ignore

        // total 12:
        // 1 land
        // 6 common
        // 3 uncommon
        // 2 rare
    }

    /**
     * New default booster configuration (after 2024 - MKM)
     */
    public void enablePlayBooster(int maxCardNumberInBooster) {
        // https://mtg.fandom.com/wiki/Play_Booster
        this.hasBoosters = true;
        this.maxCardNumberInBooster = maxCardNumberInBooster;

        // #1-6 Common
        this.numBoosterCommon = 6;

        // #7 Common or The List
        // simplify: ignore 1.5% chance of a Special Guest card
        this.numBoosterCommon++;

        // #8-10 Uncommon
        this.numBoosterUncommon = 3;

        // #12 Rare or Mythic Rare
        this.numBoosterRare = 1;
        this.ratioBoosterMythic = 8; // 12.5% chance of a mythic rare

        // #13 Basic land
        this.hasBasicLands = true;
        this.numBoosterLands = 1;

        // #11 Non-foil Wildcard (A card of any rarity from the set. Guaranteed to be non-foil.)
        // #14 Foil Wildcard (A card of any rarity from the set. Guaranteed to be foil.)
        // simplify: use U + R instead x2 wild cards
        this.numBoosterUncommon++;
        this.numBoosterRare++;
    }

    public void enableArenaBooster(int maxCardNumberInBooster) {
        // same as play booster on 2024
        enablePlayBooster(maxCardNumberInBooster);
    }

    public void enableCollectorBooster(int maxCardNumberInBooster) {
        // simplified rarity distribution
        enableCollectorBooster(maxCardNumberInBooster, 1, 5, 4, 5);
    }

    public void enableCollectorBooster(int maxCardNumberInBooster, int land, int common, int uncommon, int rare) {
        // https://mtg.fandom.com/wiki/Collector_Booster
        this.hasBoosters = true;
        this.maxCardNumberInBooster = maxCardNumberInBooster;

        this.numBoosterLands = land;
        this.hasBasicLands = land > 0;

        this.numBoosterCommon = common;
        this.numBoosterUncommon = uncommon;
        this.numBoosterRare = rare;
        this.ratioBoosterMythic = 8; // 12.5% chance of a mythic rare
    }
}
