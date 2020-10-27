/*
 * M2ReleaseActionDescriptor.java
 *
 * created at 2020-10-27 by Sascha Vogt <s.vogt@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package org.jvnet.hudson.plugins.m2release;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Descriptor;
import hudson.util.ComboBoxModel;


@Extension
public class M2ReleaseActionDescriptor extends Descriptor<M2ReleaseAction>
{
	public M2ReleaseActionDescriptor() {
		super(M2ReleaseAction.class);
	}

	public AutoCompletionCandidates doAutoCompleteSubmodule(@QueryParameter final String value, @AncestorInPath MavenModuleSet project) {
		AutoCompletionCandidates candidates = new AutoCompletionCandidates();
		Set<String> modules = new TreeSet<>();
		project.getRootModule().getChildren().forEach(m->getModuleCoordinates(m, modules));
		for (String coordinate : modules) {
			if (coordinate.toLowerCase(Locale.ROOT).contains(value.toLowerCase())) {
				candidates.add(coordinate);
			}
		}
		return candidates;
	}

	public ComboBoxModel doFillReleaseVersionItems(@QueryParameter String submodule, @AncestorInPath MavenModuleSet project) {
		ComboBoxModel candidate = new ComboBoxModel();
		if (submodule==null || submodule.trim().isEmpty()) {
			candidate.add(computeReleaseVersion(project.getRootModule()));
		} else {
			String firstSubmodule = submodule.trim();
			if (firstSubmodule.contains(",")) {
				firstSubmodule = firstSubmodule.substring(firstSubmodule.indexOf(","));
			}
			candidate.add(computeReleaseVersion(project.getModule(firstSubmodule)));
		}
		if (candidate.isEmpty()) {
			candidate.add("NaN");
		}
		return candidate;
	}

	private String computeReleaseVersion(MavenModule module) {
		String version = "NaN";

		if (module != null && StringUtils.isNotBlank(module.getVersion())) {
			try {
				DefaultVersionInfo dvi = new DefaultVersionInfo(module.getVersion());
				version = dvi.getReleaseVersionString();
			} catch (VersionParseException vpEx) {
				LOGGER.log(Level.WARNING, "Failed to compute next version.", vpEx);
				version = module.getVersion().replace("-SNAPSHOT", "");
			}
		}
		return version;
	}

	public ComboBoxModel doFillDevelopmentVersionItems(@QueryParameter String submodule, @AncestorInPath MavenModuleSet project) {
		ComboBoxModel candidate = new ComboBoxModel();
		if (submodule==null || submodule.trim().isEmpty()) {
			candidate.add(computeNextVersion(project.getRootModule()));
		} else {
			String firstSubmodule = submodule.trim();
			if (firstSubmodule.contains(",")) {
				firstSubmodule = firstSubmodule.substring(firstSubmodule.indexOf(","));
			}
			candidate.add(computeNextVersion(project.getModule(firstSubmodule)));
		}
		if (candidate.isEmpty()) {
			candidate.add("NaN-SNAPSHOT");
		}
		return candidate;
	}

	private String computeNextVersion(MavenModule module) {
		String version = "NaN-SNAPSHOT";
		if ( module != null && StringUtils.isNotBlank( module.getVersion())) {
			try {
				DefaultVersionInfo dvi = new DefaultVersionInfo( module.getVersion());
				version = dvi.getNextVersion().getSnapshotVersionString();
			} catch (Exception vpEx) {
				LOGGER.log(Level.WARNING, "Failed to compute next version.", vpEx);
			}
		}
		return version;
	}

	private void getModuleCoordinates(MavenModule m, Set<String> result)
	{
		for (MavenModule child : m.getChildren()) {
			getModuleCoordinates(child, result);
		}
		result.add(getCoordinate(m));
	}

	private String getCoordinate(MavenModule m)
	{
		return m.getGroupId()+":"+m.getArtifactId();
	}

	private static final Logger LOGGER = Logger.getLogger(M2ReleaseActionDescriptor.class.getName());
}
