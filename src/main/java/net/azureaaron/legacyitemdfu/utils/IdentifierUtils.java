package net.azureaaron.legacyitemdfu.utils;

public class IdentifierUtils {

	public static String tryParse(String id) {
		return trySplitOn(id, ':');
	}

	private static String trySplitOn(String id, char delimiter) {
		int i = id.indexOf(delimiter);
		if (i >= 0) {
			String string = id.substring(i + 1);
			if (!isPathValid(string)) {
				return null;
			} else if (i != 0) {
				String string2 = id.substring(0, i);
				return isNamespaceValid(string2) ? (string2 + ":" + string) : null;
			} else {
				return "minecraft" + ":" + string;
			}
		} else {
			return isPathValid(id) ? "minecraft" + ":" + id : null;
		}
	}
	
	private static boolean isNamespaceValid(String namespace) {
		for (int i = 0; i < namespace.length(); i++) {
			if (!isNamespaceCharacterValid(namespace.charAt(i))) {
				return false;
			}
		}

		return true;
	}
	
	private static boolean isPathValid(String path) {
		for (int i = 0; i < path.length(); i++) {
			if (!isPathCharacterValid(path.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	private static boolean isNamespaceCharacterValid(char character) {
		return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.';
	}

	private static boolean isPathCharacterValid(char character) {
		return character == '_'
			|| character == '-'
			|| character >= 'a' && character <= 'z'
			|| character >= '0' && character <= '9'
			|| character == '/'
			|| character == '.';
	}
}
