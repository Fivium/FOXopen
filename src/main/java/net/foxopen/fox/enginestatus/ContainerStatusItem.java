package net.foxopen.fox.enginestatus;

public interface ContainerStatusItem
extends StatusItem {

  StatusItem getNestedItem(String pItemMnem);

}
