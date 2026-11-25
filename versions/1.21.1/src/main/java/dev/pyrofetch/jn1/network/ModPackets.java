package dev.pyrofetch.jn1.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ModPackets {
	private ModPackets() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(SpeakPayload.ID, SpeakPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ProgressSyncPayload.ID, ProgressSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenTerminalPayload.ID, OpenTerminalPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenNotePayload.ID, OpenNotePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SaveNotePayload.ID, SaveNotePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(HugPayload.ID, HugPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TerminalActionPayload.ID, TerminalActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ShopPurchasePayload.ID, ShopPurchasePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(JennyActionPayload.ID, JennyActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenCameraPayload.ID, OpenCameraPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(PhotoDataPayload.ID, PhotoDataPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PhotoUploadPayload.ID, PhotoUploadPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PhotoRequestPayload.ID, PhotoRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CameraAimPayload.ID, CameraAimPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenVendingPayload.ID, OpenVendingPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenPanelPayload.ID, OpenPanelPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenSelectorPayload.ID, OpenSelectorPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SelectorActionPayload.ID, SelectorActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(LidarPayload.ID, LidarPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenScenesPayload.ID, OpenScenesPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ExcitationPayload.ID, ExcitationPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OrgasmePayload.ID, OrgasmePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenGenderPayload.ID, OpenGenderPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(GenderChoicePayload.ID, GenderChoicePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PogoPayload.ID, PogoPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(OnlyFapsPayload.ID, OnlyFapsPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OnlyFapsEtatPayload.ID, OnlyFapsEtatPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ScenePayload.ID, ScenePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ScenePropPayload.ID, ScenePropPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SceneJoinPayload.ID, SceneJoinPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MorphSyncPayload.ID, MorphSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenMorphPayload.ID, OpenMorphPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MorphRequestPayload.ID, MorphRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MorphGesturePayload.ID, MorphGesturePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenNotebookPayload.ID, OpenNotebookPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenCompanionPayload.ID, OpenCompanionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TalkPayload.ID, TalkPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(GasterShopPayload.ID, GasterShopPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(GasterShopActionPayload.ID, GasterShopActionPayload.CODEC);
	}
}
