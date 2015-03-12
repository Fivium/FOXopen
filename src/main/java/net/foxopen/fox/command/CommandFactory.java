package net.foxopen.fox.command;

import java.util.Collection;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.module.Mod;

public interface CommandFactory {

  Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax;

  Collection<String> getCommandElementNames();

}
