FROM maven:3.9-eclipse-temurin-11 AS builder
WORKDIR /mage/src

RUN apt-get update && apt-get install -y --no-install-recommends unzip

# Copy only POM files first to cache dependency resolution
COPY pom.xml .
COPY Mage/pom.xml Mage/
COPY Mage.Common/pom.xml Mage.Common/
COPY Mage.Server/pom.xml Mage.Server/
COPY Mage.Sets/pom.xml Mage.Sets/
COPY Mage.Client/pom.xml Mage.Client/
COPY Mage.Plugins/pom.xml Mage.Plugins/
COPY Mage.Plugins/Mage.Counter.Plugin/pom.xml Mage.Plugins/Mage.Counter.Plugin/
COPY Mage.Server.Plugins/pom.xml Mage.Server.Plugins/
COPY Mage.Server.Plugins/Mage.Deck.Constructed/pom.xml Mage.Server.Plugins/Mage.Deck.Constructed/
COPY Mage.Server.Plugins/Mage.Deck.Limited/pom.xml Mage.Server.Plugins/Mage.Deck.Limited/
COPY Mage.Server.Plugins/Mage.Game.BrawlDuel/pom.xml Mage.Server.Plugins/Mage.Game.BrawlDuel/
COPY Mage.Server.Plugins/Mage.Game.BrawlFreeForAll/pom.xml Mage.Server.Plugins/Mage.Game.BrawlFreeForAll/
COPY Mage.Server.Plugins/Mage.Game.CanadianHighlanderDuel/pom.xml Mage.Server.Plugins/Mage.Game.CanadianHighlanderDuel/
COPY Mage.Server.Plugins/Mage.Game.CommanderDuel/pom.xml Mage.Server.Plugins/Mage.Game.CommanderDuel/
COPY Mage.Server.Plugins/Mage.Game.CommanderFreeForAll/pom.xml Mage.Server.Plugins/Mage.Game.CommanderFreeForAll/
COPY Mage.Server.Plugins/Mage.Game.CustomPillarOfTheParunsDuel/pom.xml Mage.Server.Plugins/Mage.Game.CustomPillarOfTheParunsDuel/
COPY Mage.Server.Plugins/Mage.Game.FreeForAll/pom.xml Mage.Server.Plugins/Mage.Game.FreeForAll/
COPY Mage.Server.Plugins/Mage.Game.FreeformCommanderDuel/pom.xml Mage.Server.Plugins/Mage.Game.FreeformCommanderDuel/
COPY Mage.Server.Plugins/Mage.Game.FreeformCommanderFreeForAll/pom.xml Mage.Server.Plugins/Mage.Game.FreeformCommanderFreeForAll/
COPY Mage.Server.Plugins/Mage.Game.FreeformUnlimitedCommander/pom.xml Mage.Server.Plugins/Mage.Game.FreeformUnlimitedCommander/
COPY Mage.Server.Plugins/Mage.Game.MomirDuel/pom.xml Mage.Server.Plugins/Mage.Game.MomirDuel/
COPY Mage.Server.Plugins/Mage.Game.MomirGame/pom.xml Mage.Server.Plugins/Mage.Game.MomirGame/
COPY Mage.Server.Plugins/Mage.Game.OathbreakerDuel/pom.xml Mage.Server.Plugins/Mage.Game.OathbreakerDuel/
COPY Mage.Server.Plugins/Mage.Game.OathbreakerFreeForAll/pom.xml Mage.Server.Plugins/Mage.Game.OathbreakerFreeForAll/
COPY Mage.Server.Plugins/Mage.Game.PennyDreadfulCommanderFreeForAll/pom.xml Mage.Server.Plugins/Mage.Game.PennyDreadfulCommanderFreeForAll/
COPY Mage.Server.Plugins/Mage.Game.TinyLeadersDuel/pom.xml Mage.Server.Plugins/Mage.Game.TinyLeadersDuel/
COPY Mage.Server.Plugins/Mage.Game.TwoPlayerDuel/pom.xml Mage.Server.Plugins/Mage.Game.TwoPlayerDuel/
COPY Mage.Server.Plugins/Mage.Player.AI/pom.xml Mage.Server.Plugins/Mage.Player.AI/
COPY Mage.Server.Plugins/Mage.Player.AI.DraftBot/pom.xml Mage.Server.Plugins/Mage.Player.AI.DraftBot/
COPY Mage.Server.Plugins/Mage.Player.AI.MA/pom.xml Mage.Server.Plugins/Mage.Player.AI.MA/
COPY Mage.Server.Plugins/Mage.Player.AIMCTS/pom.xml Mage.Server.Plugins/Mage.Player.AIMCTS/
COPY Mage.Server.Plugins/Mage.Player.Human/pom.xml Mage.Server.Plugins/Mage.Player.Human/
COPY Mage.Server.Plugins/Mage.Tournament.BoosterDraft/pom.xml Mage.Server.Plugins/Mage.Tournament.BoosterDraft/
COPY Mage.Server.Plugins/Mage.Tournament.Constructed/pom.xml Mage.Server.Plugins/Mage.Tournament.Constructed/
COPY Mage.Server.Plugins/Mage.Tournament.Sealed/pom.xml Mage.Server.Plugins/Mage.Tournament.Sealed/
COPY Mage.Server.Console/pom.xml Mage.Server.Console/
COPY Mage.Tests/pom.xml Mage.Tests/
COPY Mage.Verify/pom.xml Mage.Verify/
COPY Mage.Reports/pom.xml Mage.Reports/

# Copy local repository (contains jspf-core and other vendored deps)
COPY repository/ repository/

# Download all dependencies (cached until a pom.xml changes)
RUN mvn dependency:go-offline -B || true

# Now copy full source and build
COPY . .
RUN mvn clean install -U -DskipTests && \
    cd Mage.Server && \
    mvn assembly:single && \
    mkdir -p /mage/server && \
    unzip target/mage-server.zip -d /mage/server && \
    cp target/mage-server.jar /mage/server/lib/ && \
    echo "=== Server root ===" && ls -la /mage/server/ && \
    echo "=== Server lib ===" && ls -la /mage/server/lib/mage-server* && \
    (echo "=== Server plugins ===" && ls -la /mage/server/plugins/ || echo "No plugins dir")

FROM eclipse-temurin:11-jre

ENV XMAGE_DOCKER_SERVER_ADDRESS="0.0.0.0" \
    XMAGE_DOCKER_PORT="17171" \
    XMAGE_DOCKER_SECONDARY_BIND_PORT="17179" \
    XMAGE_DOCKER_MAX_SECONDS_IDLE="600" \
    XMAGE_DOCKER_AUTHENTICATION_ACTIVATED="false" \
    XMAGE_DOCKER_SERVER_NAME="mage-server" \
    JAVA_MIN_MEMORY="256M" \
    JAVA_MAX_MEMORY="512M"

EXPOSE 17171 17179

WORKDIR /xmage
COPY --from=builder /mage/server/ .
COPY docker/dockerStartServer.sh /xmage/

RUN chmod +x /xmage/startServer.sh /xmage/dockerStartServer.sh

CMD ["./dockerStartServer.sh"]
