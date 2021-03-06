package com.jmisur.dog.tree.generated;

import javax.annotation.Generated;

import com.jmisur.dog.tree.House;
import com.jmisur.dog.tree.Tree;

/**
 * Generated by JAnnocessor
 */
@Generated("Easily with JAnnocessor :)")
public class MyMappingDefinitionGenerated {

	public MyMappingDefinition converter = new MyMappingDefinition();

	public House map(Tree source) {
		House dest = new House();
		dest.setSquaredmeters(converter.toDouble(source.getHeight()));
		dest.setLength(converter.toLong(source.getIsHigh()));
		dest.setWindowCount(source.getLeafCount());
		return dest;
	}

	public Tree map(House source) {
		Tree dest = new Tree();
		dest.setHeight(converter.toLong(source.getSquaredmeters()));
		dest.setIsHigh(converter.toBoolean(source.getLength()));
		dest.setLeafCount(source.getWindowCount());
		return dest;
	}

}