
# react-native-imageview

## Getting started

`$ npm install react-native-imageview --save`

### Mostly automatic installation

`$ react-native link react-native-imageview`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-imageview` and add `RNImageview.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNImageview.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.focus.imageview.RNImageviewPackage;` to the imports at the top of the file
  - Add `new RNImageviewPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-imageview'
  	project(':react-native-imageview').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-imageview/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-imageview')
  	```


## Usage
```javascript
import RNImageview from 'react-native-imageview';

// TODO: What to do with the module?
RNImageview;
```
  