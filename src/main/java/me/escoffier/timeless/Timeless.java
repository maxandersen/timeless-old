package me.escoffier.timeless;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

import javax.inject.Inject;

@QuarkusMain
@CommandLine.Command(
        name = "timeless", mixinStandardHelpOptions = true,
        subcommands = {
                SyncCommand.class,
                ReportCompletedCommand.class,
                WeeklyCommand.class
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
            args = new String[] { "sync" };
        }
        Quarkus.run(Timeless.class, args);
    }

}
