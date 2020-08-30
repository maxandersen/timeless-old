package me.escoffier.timeless;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.Task;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@QuarkusMain
@CommandLine.Command(
        name = "timeless", mixinStandardHelpOptions = true,
        subcommands = {
                SyncCommand.class
        }
)
public class Timeless implements QuarkusApplication {

    @Inject
    CommandLine.IFactory factory;

    @Override
    public int run(String... args) {
        return new CommandLine(this, factory).execute(args);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[] { "sync"};
        }
        Quarkus.run(Timeless.class, args);
    }

}
