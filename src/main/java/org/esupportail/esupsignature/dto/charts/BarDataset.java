/*
 * Copyright © 2023 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupsignature.dto.charts;

import software.xdev.chartjs.model.dataset.BackgroundBorderHoverDataset;
import software.xdev.chartjs.model.dataset.BigDecimalDataset;
import software.xdev.chartjs.model.enums.BorderSkipped;
import software.xdev.chartjs.model.objects.OptionalArray;

import java.math.BigDecimal;
import java.util.List;


public class BarDataset extends BackgroundBorderHoverDataset<BarDataset, BigDecimal>
	implements BigDecimalDataset<BarDataset>
{
	private String label;
	private String xAxisID;
	private String yAxisID;
	private final List<BorderSkipped> borderSkipped = new OptionalArray<>();
	private String stack;
	
	
	public String getLabel()
	{
		return this.label;
	}
	
	public BarDataset setLabel(final String label)
	{
		this.label = label;
		return this;
	}
	
	public String getXAxisID()
	{
		return this.xAxisID;
	}
	
	public BarDataset setXAxisID(final String xAxisID)
	{
		this.xAxisID = xAxisID;
		return this;
	}
	
	public String getYAxisID()
	{
		return this.yAxisID;
	}
	
	public BarDataset setYAxisID(final String yAxisID)
	{
		this.yAxisID = yAxisID;
		return this;
	}
	
	public List<BorderSkipped> getBorderSkipped()
	{
		return this.borderSkipped;
	}
	
	public BarDataset addBorderSkipped(final BorderSkipped borderSkipped)
	{
		this.borderSkipped.add(borderSkipped);
		return this;
	}
	
	public BarDataset setBorderSkipped(final List<BorderSkipped> borderSkipped)
	{
		this.borderSkipped.clear();
		if(borderSkipped != null)
		{
			this.borderSkipped.addAll(borderSkipped);
		}
		return this;
	}

	public String getStack() {
		return stack;
	}

	public BarDataset setStack(String stack) {
		this.stack = stack;
		return this;
	}
}
