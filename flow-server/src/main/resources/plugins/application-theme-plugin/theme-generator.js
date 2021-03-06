/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * This file handles the generation of the '[theme-name].js' to
 * the theme/[theme-name] folder according to properties from 'theme.json'.
 */
const glob = require('glob');
const path = require('path');

// The contents of a global CSS file with this name in a theme is always added to
// the document. E.g. @font-face must be in this
const themeFileAlwaysAddToDocument = 'document.css';

const headerImport = ``;

const injectGlobalCssMethod = `
// target: Document | ShadowRoot
export const injectGlobalCss = (css, target) => {
  const sheet = new CSSStyleSheet();
  sheet.replaceSync(css);
  target.adoptedStyleSheets = [...target.adoptedStyleSheets, sheet];
};
`;

/**
 * Generate the [themeName].js file for themeFolder which collects all required information from the folder.
 *
 * @param {string} themeFolder folder of the theme
 * @param {string} themeName name of the handled theme
 * @returns {string} theme file content
 */
function generateThemeFile(themeFolder, themeName) {
  const globalFiles = glob.sync('*.css', {
    cwd: themeFolder,
    nodir: true,
  });

  let themeFile = headerImport;

  themeFile += injectGlobalCssMethod;

  const imports = [];
  const globalCssCode = [];

  globalFiles.forEach((global) => {
    const filename = path.basename(global);
    const variable = camelCase(filename);
    imports.push(`import ${variable} from './${filename}';\n`);
    if (filename == themeFileAlwaysAddToDocument) {
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), document);\n`);
    }
    globalCssCode.push(`injectGlobalCss(${variable}.toString(), target);\n`);
  });

  const themeIdentifier = '_vaadinds_' + themeName + '_';
  const globalCssFlag = themeIdentifier + 'globalCss';

  themeFile += imports.join('');

// Don't format as the generated file formatting will get wonky!
  const themeFileApply = `export const applyTheme = (target) => {
  if (!target['${globalCssFlag}']) {
    ${globalCssCode.join('')}
    target['${globalCssFlag}'] = true;
  }
}
`;

  themeFile += themeFileApply;

  return themeFile;
};

/**
 * Make given string into camelCase.
 *
 * @param {string} str string to make into cameCase
 * @returns {string} camelCased version
 */
function camelCase(str) {
  return str.replace(/(?:^\w|[A-Z]|\b\w)/g, function(word, index) {
    return index === 0 ? word.toLowerCase() : word.toUpperCase();
  }).replace(/\s+/g, '').replace(/\./g, '');
}

module.exports = generateThemeFile;
